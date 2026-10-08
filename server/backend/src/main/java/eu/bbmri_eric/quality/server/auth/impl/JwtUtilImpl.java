package eu.bbmri_eric.quality.server.auth.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token generation and validation using JJWT library. Provides simple JWT
 * functionality without OAuth2 complexity.
 */
@Service
public class JwtUtilImpl implements eu.bbmri_eric.quality.server.auth.JwtUtil {

  private static final Logger logger = LoggerFactory.getLogger(JwtUtilImpl.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final SecretKey key;
  private final long jwtExpiration;

  JwtUtilImpl(@Value("${app.jwt.expiration:3600000}") long jwtExpiration) {
    this.key = Jwts.SIG.HS256.key().build();
    this.jwtExpiration = jwtExpiration;
  }

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
        .issuer("quality-server")
        .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(key)
        .compact();
  }

  @Override
  public String extractUsername(String token) {
    return extractClaims(token).getSubject();
  }

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
   * Extracts the issuer from a JWT token without verifying signature. Used to determine token type
   * and route to the appropriate authentication provider.
   *
   * @param token JWT token
   * @return issuer claim value (never null)
   * @throws IllegalArgumentException if token is malformed or issuer claim is missing
   */
  public String extractIssuer(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException(
          "Invalid JWT structure: expected 3 parts, got " + parts.length);
    }

    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
    JsonNode payloadNode;
    try {
      payloadNode = OBJECT_MAPPER.readTree(payloadJson);
    } catch (Exception e) {
      throw new JwtException("Failed to parse JWT token: " + e.getMessage(), e);
    }

    String issuer = payloadNode.has("iss") ? payloadNode.get("iss").asText() : null;

    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("JWT token missing or empty 'iss' (issuer) claim");
    }

    logger.debug("Extracted issuer from token: '{}'", issuer);
    return issuer;
  }

  /**
   * Extracts claims from JWT token. This method performs signature verification using the secret
   * key.
   *
   * @param token JWT token
   * @return JWT claims
   * @throws JwtException if signature verification fails or token is malformed
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
