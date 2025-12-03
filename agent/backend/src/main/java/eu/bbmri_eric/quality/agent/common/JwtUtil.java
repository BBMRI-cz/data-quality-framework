package eu.bbmri_eric.quality.agent.common;

import org.springframework.security.core.Authentication;

/**
 * Utility interface for JWT (JSON Web Token) operations.
 *
 * <p>This interface defines contracts for generating, validating, and extracting information from
 * JWT tokens used for authentication and authorization.
 */
public interface JwtUtil {
  /**
   * Generates a JWT token based on the provided authentication.
   *
   * @param authentication the authentication object containing user details and authorities
   * @return the generated JWT token as a string
   */
  String generateToken(Authentication authentication);

  /**
   * Extracts the username from a JWT token.
   *
   * @param token the JWT token to extract the username from
   * @return the username contained in the token
   */
  String extractUsername(String token);

  /**
   * Validates a JWT token against a provided username.
   *
   * @param token the JWT token to validate
   * @param username the username to validate against
   * @return true if the token is valid and matches the username, false otherwise
   */
  boolean validateToken(String token, String username);
}
