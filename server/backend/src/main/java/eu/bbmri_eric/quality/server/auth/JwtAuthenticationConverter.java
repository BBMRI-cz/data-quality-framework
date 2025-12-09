package eu.bbmri_eric.quality.server.auth;

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
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationConverter.class);

  private final UserService userService;
  private final OAuth2TokenValidator<Jwt> validator;

  public JwtAuthenticationConverter(@Lazy UserService userService) {
    this.userService = userService;
    this.validator = createValidator();
  }

  /**
   * Creates an OAuth2TokenValidator that checks JWT timestamps and required claims.
   *
   * @return configured OAuth2TokenValidator
   */
  private OAuth2TokenValidator<Jwt> createValidator() {
    OAuth2TokenValidator<Jwt> timestampValidator =
        new JwtTimestampValidator(Duration.ofSeconds(10));

    OAuth2TokenValidator<Jwt> subjectValidator =
        new JwtClaimValidator<String>("sub", subject -> subject != null && !subject.isBlank());

    return token -> {
      OAuth2TokenValidatorResult result = timestampValidator.validate(token);
      if (result.hasErrors()) {
        return result;
      }
      return subjectValidator.validate(token);
    };
  }

  /**
   * Converts the given Jwt into a JwtAuthenticationToken after validation and user retrieval.
   *
   * @param jwt the JWT token to convert
   * @return JwtAuthenticationToken representing the authenticated user
   * @throws IllegalArgumentException if JWT validation fails
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

    String subjectId = jwt.getSubject();
    String principalName = extractPrincipalName(jwt);

    logger.debug("Processing OIDC token for subject: {}", subjectId);

    UserDTO user = userService.findOrCreateUserBySubjectId(subjectId, principalName);

    Set<GrantedAuthority> authorities = extractAuthorities(user);

    logger.debug(
        "OIDC authentication successful for user: {} with {} authorities",
        user.getUsername(),
        authorities.size());

    return new JwtAuthenticationToken(jwt, authorities, principalName);
  }

  /**
   * Extracts principal name from JWT, preferring 'preferred_username' claim.
   *
   * @param jwt the JWT token
   * @return principal name (never null or blank)
   */
  private String extractPrincipalName(Jwt jwt) {
    return Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
        .filter(name -> !name.isBlank())
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
}
