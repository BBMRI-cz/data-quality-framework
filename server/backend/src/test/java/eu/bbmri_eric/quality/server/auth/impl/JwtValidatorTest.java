package eu.bbmri_eric.quality.server.auth.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtValidatorTest {

  private final JwtValidator jwtValidator = new JwtValidator();

  @Test
  void validate_withSupportedIdentityClaimsAndNoLegacyIdentityClaim_succeeds() {
    assertDoesNotThrow(() -> jwtValidator.validate(validTokenWithIdentity("sub", "1000@bbmri.eu")));
    assertDoesNotThrow(() -> jwtValidator.validate(validTokenWithIdentity("client_id", "client")));
  }

  @Test
  void validate_withExpiredToken_throwsIllegalArgumentException() {
    Instant now = Instant.now();
    Jwt expiredToken =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("1000@bbmri.eu")
            .issuedAt(now.minusSeconds(600))
            .expiresAt(now.minusSeconds(300))
            .build();

    assertThrows(IllegalArgumentException.class, () -> jwtValidator.validate(expiredToken));
  }

  private static Jwt validTokenWithIdentity(String claimName, String claimValue) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim(claimName, claimValue)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
