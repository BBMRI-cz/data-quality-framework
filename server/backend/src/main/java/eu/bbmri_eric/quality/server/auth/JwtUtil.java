package eu.bbmri_eric.quality.server.auth;

import org.springframework.security.core.Authentication;

public interface JwtUtil {
  /**
   * Generates a JWT token for the authenticated user.
   *
   * @param authentication the authentication object containing user details
   * @return JWT token as a string
   */
  String generateToken(Authentication authentication);

  /**
   * Extracts username from JWT token. This method validates the signature as part of the extraction
   * process.
   *
   * @param token JWT token
   * @return username
   * @throws io.jsonwebtoken.JwtException if a token is invalid or signature verification fails
   */
  String extractUsername(String token);

  /**
   * Validates JWT token including signature verification.
   *
   * @param token JWT token
   * @param username username to validate against
   * @return true if token is valid and signature is verified
   */
  boolean validateToken(String token, String username);
}
