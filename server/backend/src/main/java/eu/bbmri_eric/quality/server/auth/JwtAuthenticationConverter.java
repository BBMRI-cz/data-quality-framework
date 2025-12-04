package eu.bbmri_eric.quality.server.auth;

import eu.bbmri_eric.quality.server.user.UserDTO;
import eu.bbmri_eric.quality.server.user.UserService;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
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

  public JwtAuthenticationConverter(@Lazy UserService userService) {
    this.userService = userService;
  }

  @Override
  public JwtAuthenticationToken convert(Jwt jwt) {
    Objects.requireNonNull(jwt, "JWT cannot be null");

    String subjectId = extractAndValidateSubject(jwt);
    String principalName = extractPrincipalName(jwt);
    validateJwtClaims(jwt);

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
   * Extracts and validates the subject claim from JWT.
   *
   * @param jwt the JWT token
   * @return validated subject ID
   * @throws IllegalArgumentException if subject is invalid
   */
  private String extractAndValidateSubject(Jwt jwt) {
    String subjectId = jwt.getSubject();

    if (subjectId == null || subjectId.isBlank()) {
      throw new IllegalArgumentException("JWT must contain non-empty 'sub' claim");
    }

    return subjectId;
  }

  /**
   * Extracts principal name from JWT, preferring 'preferred_username' claim.
   *
   * @param jwt the JWT token
   * @return principal name (never null or blank)
   */
  private String extractPrincipalName(Jwt jwt) {
    String principalName =
        Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
            .filter(name -> !name.isBlank())
            .orElseGet(jwt::getSubject);

    return principalName;
  }

  /**
   * Validates JWT claims for security.
   *
   * @param jwt the JWT token
   * @throws IllegalArgumentException if validation fails
   */
  private void validateJwtClaims(Jwt jwt) {
    Instant expiresAt = jwt.getExpiresAt();
    if (expiresAt == null) {
      throw new IllegalArgumentException("JWT missing expiration claim");
    }

    if (expiresAt.isBefore(Instant.now())) {
      throw new IllegalArgumentException("JWT has expired");
    }

    Instant issuedAt = jwt.getIssuedAt();
    if (issuedAt != null && issuedAt.isAfter(Instant.now().plusSeconds(10))) {
      throw new IllegalArgumentException("JWT issued in the future");
    }

    logger.debug("JWT claims validation passed for issuer: {}", jwt.getIssuer());
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
