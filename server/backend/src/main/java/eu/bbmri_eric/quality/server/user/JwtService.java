package eu.bbmri_eric.quality.server.user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token generation and validation using JJWT library. Provides simple JWT
 * functionality without OAuth2 complexity.
 */
@Service
public class JwtService {

  private final SecretKey key;
  private final long jwtExpiration;

  public JwtService(@Value("${app.jwt.expiration:3600000}") long jwtExpiration) {
    this.key = Jwts.SIG.HS256.key().build();
    this.jwtExpiration = jwtExpiration;
  }

  /**
   * Generates a JWT token for the authenticated user.
   *
   * @param authentication the authentication object containing user details
   * @return JWT token as a string
   */
  public String generateToken(Authentication authentication) {
    String authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
    return Jwts.builder()
        .subject(authentication.getName())
        .claim("authorities", authorities)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(key)
        .compact();
  }

  /**
   * Extracts username from JWT token.
   *
   * @param token JWT token
   * @return username
   */
  public String extractUsername(String token) {
    return extractClaims(token).getSubject();
  }

  /**
   * Validates JWT token.
   *
   * @param token JWT token
   * @param username username to validate against
   * @return true if token is valid
   */
  public boolean validateToken(String token, String username) {
    try {
      String tokenUsername = extractUsername(token);
      return username.equals(tokenUsername) && !isTokenExpired(token);
    } catch (Exception e) {
      return false;
    }
  }

  private Claims extractClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  private boolean isTokenExpired(String token) {
    return extractClaims(token).getExpiration().before(new Date());
  }
}
