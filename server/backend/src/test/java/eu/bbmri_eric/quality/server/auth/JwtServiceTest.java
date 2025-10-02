package eu.bbmri_eric.quality.server.auth;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import java.util.Base64;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Unit tests for JwtService focusing on signature validation and token security. */
class JwtServiceTest {

  private JwtUtil jwtService;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    jwtService = new JwtUtil(3600000);
    authentication =
        new UsernamePasswordAuthenticationToken(
            "testuser",
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Test
  @DisplayName("Should validate signature correctly for valid tokens")
  void validateToken_withValidSignature_returnsTrue() {
    String token = jwtService.generateToken(authentication);
    assertTrue(jwtService.validateToken(token, "testuser"));
  }

  @Test
  @DisplayName("Should reject tokens with tampered payload")
  void validateToken_withTamperedPayload_returnsFalse() {
    String token = jwtService.generateToken(authentication);
    String[] tokenParts = token.split("\\.");

    String tamperedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                "{\"sub\":\"hacker\",\"iat\":1234567890,\"exp\":9999999999}".getBytes());
    String tamperedToken = tokenParts[0] + "." + tamperedPayload + "." + tokenParts[2];

    assertFalse(jwtService.validateToken(tamperedToken, "hacker"));
  }

  @Test
  @DisplayName("Should reject tokens with tampered signature")
  void validateToken_withTamperedSignature_returnsFalse() {
    String token = jwtService.generateToken(authentication);
    String[] tokenParts = token.split("\\.");

    String tamperedSignature = "invalidSignature123";
    String tamperedToken = tokenParts[0] + "." + tokenParts[1] + "." + tamperedSignature;

    assertFalse(jwtService.validateToken(tamperedToken, "testuser"));
  }

  @Test
  @DisplayName("Should reject completely malformed tokens")
  void validateToken_withMalformedToken_returnsFalse() {
    assertFalse(jwtService.validateToken("not.a.valid.jwt.token", "testuser"));
    assertFalse(jwtService.validateToken("invalid", "testuser"));
    assertFalse(jwtService.validateToken("", "testuser"));
    assertFalse(jwtService.validateToken("a.b", "testuser"));
  }

  @Test
  @DisplayName("Should throw SignatureException when extracting username from tampered token")
  void extractUsername_withTamperedToken_throwsSignatureException() {
    String token = jwtService.generateToken(authentication);
    String[] tokenParts = token.split("\\.");

    String tamperedPayload =
        Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"hacker\"}".getBytes());
    String tamperedToken = tokenParts[0] + "." + tamperedPayload + "." + tokenParts[2];

    assertThrows(
        SignatureException.class,
        () -> {
          jwtService.extractUsername(tamperedToken);
        });
  }

  @Test
  @DisplayName("Should validate token with correct username but reject wrong username")
  void validateToken_withUsernameValidation_worksCorrectly() {
    String token = jwtService.generateToken(authentication);

    assertTrue(jwtService.validateToken(token, "testuser"));
    assertFalse(jwtService.validateToken(token, "wronguser"));
  }

  @Test
  @DisplayName("Should generate tokens that can be validated by the same service instance")
  void generateAndValidateToken_sameServiceInstance_worksCorrectly() {
    String token = jwtService.generateToken(authentication);

    String extractedUsername = jwtService.extractUsername(token);
    assertEquals("testuser", extractedUsername);

    assertTrue(jwtService.validateToken(token, "testuser"));
  }

  @Test
  @DisplayName("Should fail validation when different service instances have different keys")
  void validateToken_withDifferentServiceInstance_fails() {
    String token = jwtService.generateToken(authentication);

    JwtUtil differentService = new JwtUtil(3600000);

    assertFalse(differentService.validateToken(token, "testuser"));

    assertThrows(
        JwtException.class,
        () -> {
          differentService.extractUsername(token);
        });
  }

  @Test
  @DisplayName("Should handle token without proper structure")
  void validateToken_withImproperStructure_returnsFalse() {
    assertFalse(jwtService.validateToken("onlyonepart", "testuser"));
    assertFalse(jwtService.validateToken("two.parts", "testuser"));
    assertFalse(jwtService.validateToken("four.parts.are.invalid", "testuser"));
  }
}
