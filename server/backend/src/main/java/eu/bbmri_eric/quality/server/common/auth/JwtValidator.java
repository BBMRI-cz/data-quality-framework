package eu.bbmri_eric.quality.server.common.auth;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.stereotype.Component;

/**
 * Validates JWT tokens by checking timestamps and required claims. Supports both authorization code
 * flow (with 'sub') and client-credentials flow (with 'client_id').
 */
@Component
class JwtValidator {

  private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);
  private static final Duration CLOCK_SKEW = Duration.ofSeconds(10);

  private final OAuth2TokenValidator<Jwt> validator;

  public JwtValidator() {
    this.validator = createValidator();
  }

  /**
   * Validates the given JWT token.
   *
   * @param jwt the JWT token to validate
   * @throws IllegalArgumentException if JWT validation fails
   */
  public void validate(Jwt jwt) {
    OAuth2TokenValidatorResult result = validator.validate(jwt);
    if (result.hasErrors()) {
      String errors =
          result.getErrors().stream()
              .map(OAuth2Error::getDescription)
              .collect(java.util.stream.Collectors.joining(", "));
      logger.warn("JWT validation failed: {}", errors);
      throw new IllegalArgumentException("JWT validation failed: " + errors);
    }
  }

  /**
   * Creates an OAuth2TokenValidator that checks JWT timestamps and required claims.
   *
   * @return configured OAuth2TokenValidator
   */
  private OAuth2TokenValidator<Jwt> createValidator() {
    OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(CLOCK_SKEW);

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
}
