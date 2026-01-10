package eu.bbmri_eric.quality.server.common.auth;

import eu.bbmri_eric.quality.server.user.UserDTO;
import eu.bbmri_eric.quality.server.user.UserService;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Converts a Jwt token into a JwtAuthenticationToken by validating claims, retrieving or creating
 * the associated user, and extracting authorities.
 */
@Component
class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationConverter.class);

  private enum OAuth2FlowType {
    CLIENT_CREDENTIALS,
    AUTHORIZATION_CODE
  }

  private final UserService userService;
  private final JwtValidator jwtValidator;

  public JwtAuthenticationConverter(@Lazy UserService userService, JwtValidator jwtValidator) {
    this.userService = userService;
    this.jwtValidator = jwtValidator;
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

    jwtValidator.validate(jwt);

    record TokenIdentity(String identityId, String username) {}

    OAuth2FlowType flowType = determineFlowType(jwt);
    TokenIdentity identity =
        switch (flowType) {
          case CLIENT_CREDENTIALS -> {
            String clientId = jwt.getClaimAsString("client_id");
            logger.debug("Processing client-credentials token for client: {}", clientId);
            yield new TokenIdentity(clientId, clientId);
          }
          case AUTHORIZATION_CODE -> {
            String subjectId = jwt.getSubject();
            if (subjectId == null || subjectId.isBlank()) {
              throw new IllegalArgumentException(
                  "JWT from authorization code flow must contain 'sub' claim");
            }
            String userName = extractUsername(jwt);
            logger.debug(
                "Processing authorization code token for user: {} (sub: {})",
                userName,
                subjectId);
            yield new TokenIdentity(subjectId, userName);
          }
        };

    String identityId = identity.identityId();
    String username = identity.username();

    UserDTO user;
    try {
      user = userService.findBySubjectId(identityId);
    } catch (UsernameNotFoundException e) {
      logger.debug("User not found for subject ID: {}, creating new user", identityId);
      user = userService.createBySubjectId(identityId, username);
    }

    Set<GrantedAuthority> authorities = extractAuthorities(user);

    return new JwtAuthenticationToken(jwt, authorities, username);
  }

  /**
   * Extracts username from JWT, preferring 'preferred_username', then 'sub' as fallback.
   *
   * @param jwt the JWT token
   * @return username (never null or blank)
   */
  private String extractUsername(Jwt jwt) {
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

  /**
   * Determines the OAuth2 flow type based on JWT claims.
   *
   * @param jwt the JWT token
   * @return OAuth2FlowType (CLIENT_CREDENTIALS or AUTHORIZATION_CODE)
   */
  private OAuth2FlowType determineFlowType(Jwt jwt) {
    String clientId = jwt.getClaimAsString("client_id");
    String scope = jwt.getClaimAsString("scope");
    boolean isClientCredentials = clientId != null && scope != null && !scope.contains("openid");
    return isClientCredentials ? OAuth2FlowType.CLIENT_CREDENTIALS : OAuth2FlowType.AUTHORIZATION_CODE;
  }
}
