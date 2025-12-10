package eu.bbmri_eric.quality.server.common.auth;

import eu.bbmri_eric.quality.server.user.UserDTO;
import eu.bbmri_eric.quality.server.user.UserService;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Converts a Jwt token into a JwtAuthenticationToken by validating claims, retrieving or creating
 * the associated user, and extracting authorities.
 */
@Component
class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationConverter.class);

  private final UserService userService;
  private final OAuth2TokenValidator<Jwt> validator;

  public JwtAuthenticationConverter(@Lazy UserService userService) {
    this.userService = userService;
    this.validator = createValidator();
  }

  /**
   * Creates an OAuth2TokenValidator that checks JWT timestamps and required claims. Supports both
   * authorization code flow (with 'sub') and client-credentials flow (with 'client_id').
   *
   * @return configured OAuth2TokenValidator
   */
  private OAuth2TokenValidator<Jwt> createValidator() {
    OAuth2TokenValidator<Jwt> timestampValidator =
        new JwtTimestampValidator(Duration.ofSeconds(10));

    OAuth2TokenValidator<Jwt> identityValidator =
        new JwtClaimValidator<>("identity", (ignored) -> true);

    return token -> {
      OAuth2TokenValidatorResult result = timestampValidator.validate(token);
      if (result.hasErrors()) {
        return result;
      }
      return identityValidator.validate(token);
    };
  }

  /**
   * Converts the given Jwt into a JwtAuthenticationToken after validation and user retrieval.
   * Supports both authorization code flow (user authentication) and client-credentials flow
   * (service/client authentication).
   *
   * @param jwt the JWT token to convert
   * @return JwtAuthenticationToken representing the authenticated user or client
   * @throws IllegalArgumentException if JWT validation fails or required claims are missing
   */
  @Override
  public JwtAuthenticationToken convert(@NonNull Jwt jwt) {
    Objects.requireNonNull(jwt, "JWT cannot be null");

    OAuth2TokenValidatorResult result = validator.validate(jwt);
    if (result.hasErrors()) {
      String errors =
          result.getErrors().stream()
              .map(OAuth2Error::getDescription)
              .collect(Collectors.joining(", "));
      logger.warn("JWT validation failed: {}", errors);
      throw new IllegalArgumentException("JWT validation failed: " + errors);
    }

    String identityId;
    String username;

    if (isClientCredentials(jwt)) {
      identityId = jwt.getClaimAsString("client_id");
      username = identityId;
      logger.debug("Processing client-credentials token for client: {}", identityId);
    } else {
      identityId = jwt.getSubject();

      if (identityId == null || identityId.isBlank()) {
        throw new IllegalArgumentException(
            "JWT from authorization code flow must contain 'sub' claim");
      }

      username = extractUsername(jwt);
      logger.debug(
          "Processing authorization code token for user: {} (sub: {})", username, identityId);
    }

    UserDTO user =
        userService
            .findBySubjectId(identityId)
            .orElseGet(() -> userService.createBySubjectId(identityId, username));

    Set<GrantedAuthority> authorities = extractAuthorities(user);

    return new JwtAuthenticationToken(jwt, authorities, username);
  }

  /**
   * Extracts username from JWT, preferring 'email', then 'preferred_username', then 'sub' as
   * fallback.
   *
   * @param jwt the JWT token
   * @return username (never null or blank)
   */
  private String extractUsername(Jwt jwt) {
    return Optional.ofNullable(jwt.getClaimAsString("email"))
        .filter(name -> !name.isBlank())
        .or(
            () ->
                Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                    .filter(name -> !name.isBlank()))
        .orElseGet(jwt::getSubject);
  }

  /**
   * Extracts authorities from user roles as an immutable set.
   *
   * @param user the user DTO
   * @return immutable set of granted authorities
   */
  private Set<GrantedAuthority> extractAuthorities(UserDTO user) {
    if (user.getRoles() == null || user.getRoles().isEmpty()) {
      logger.warn("User {} has no roles assigned", user.getUsername());
      return Collections.emptySet();
    }

    return user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
        .collect(Collectors.toUnmodifiableSet());
  }

  private boolean isClientCredentials(Jwt jwt) {
    String clientId = jwt.getClaimAsString("client_id");
    String scope = jwt.getClaimAsString("scope");
    return clientId != null && scope != null && !scope.contains("openid");
  }
}
