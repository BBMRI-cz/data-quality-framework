package eu.bbmri_eric.quality.agent.common.impl;

import eu.bbmri_eric.quality.agent.common.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token generation and validation using JJWT library. Provides simple JWT
 * functionality without OAuth2 complexity.
 */
@Service
class JwtUtilImpl implements JwtUtil {

  private static final Logger logger = LoggerFactory.getLogger(JwtUtilImpl.class);

  private final SecretKey key;
  private final long jwtExpiration;

  JwtUtilImpl() {
    this.key = Jwts.SIG.HS256.key().build();
    this.jwtExpiration = 3600000L;
  }

  /**
   * Generates a JWT token for the authenticated user.
   *
   * @param authentication the authentication object containing user details
   * @return JWT token as a string
   */
  @Override
  public String generateToken(Authentication authentication) {
    String authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

    return Jwts.builder()
        .header()
        .type("JWT")
        .and()
        .subject(authentication.getName())
        .claim("authorities", authorities)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(key)
        .compact();
  }

  /**
   * Extracts username from JWT token. This method validates the signature as part of the extraction
   * process.
   *
   * @param token JWT token
   * @return username
   * @throws JwtException if token is invalid or signature verification fails
   */
  @Override
  public String extractUsername(String token) {
    return extractClaims(token).getSubject();
  }

  /**
   * Validates JWT token including signature verification.
   *
   * @param token JWT token
   * @param username username to validate against
   * @return true if token is valid and signature is verified
   */
  @Override
  public boolean validateToken(String token, String username) {
    try {
      String tokenUsername = extractUsername(token);
      boolean isValid = username.equals(tokenUsername) && !isTokenExpired(token);
      logger.debug("Token validation for user '{}': {}", username, isValid);
      return isValid;
    } catch (SignatureException e) {
      logger.warn("JWT signature validation failed: {}", e.getMessage());
      return false;
    } catch (JwtException e) {
      logger.warn("JWT token validation failed: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      logger.error("Unexpected error during token validation: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Extracts claims from JWT token. This method performs signature verification using the secret
   * key.
   *
   * @param token JWT token
   * @return JWT claims
   * @throws io.jsonwebtoken.JwtException if signature verification fails or token is malformed
   */
  private Claims extractClaims(String token) {
    // The .verifyWith(key) call ensures signature verification
    // parseSignedClaims() will throw SignatureException if signature is invalid
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  private boolean isTokenExpired(String token) {
    return extractClaims(token).getExpiration().before(new Date());
  }
}
