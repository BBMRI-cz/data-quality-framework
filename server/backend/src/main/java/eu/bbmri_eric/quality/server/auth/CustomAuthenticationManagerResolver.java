package eu.bbmri_eric.quality.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

/**
 * Resolves an AuthenticationManager per request based on the JWT token's issuer claim. Supports
 * multiple authentication methods: Internal tokens: issuer="quality-server" - uses custom JWT
 * validation with UserDetailsService OIDC tokens: issuer=configured OIDC provider - uses standard
 * OAuth2 JWT validation
 */
public class CustomAuthenticationManagerResolver
    implements AuthenticationManagerResolver<HttpServletRequest> {

  private static final Logger logger =
      LoggerFactory.getLogger(CustomAuthenticationManagerResolver.class);
  private static final String INTERNAL_ISSUER = "quality-server";

  private final JwtUtil jwtUtil;
  private final Map<String, AuthenticationManager> authManagers;
  private final AuthenticationManager defaultAuthManager;

  public CustomAuthenticationManagerResolver(
      JwtUtil jwtUtil,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      UserDetailsService userDetailsService,
      String oidcIssuerUri) {
    this.jwtUtil = jwtUtil;
    this.authManagers = new HashMap<>();

    InternalTokenAuthenticationProvider internalProvider =
        new InternalTokenAuthenticationProvider(jwtUtil, userDetailsService);
    AuthenticationManager internalAuthManager = new ProviderManager(internalProvider);
    authManagers.put(INTERNAL_ISSUER, internalAuthManager);
    this.defaultAuthManager = internalAuthManager;
    logger.info("Registered internal token authentication for issuer: {}", INTERNAL_ISSUER);

    if (oidcIssuerUri != null && !oidcIssuerUri.isBlank()) {
      try {
        JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(oidcIssuerUri);
        JwtAuthenticationProvider oidcProvider = new JwtAuthenticationProvider(jwtDecoder);
        oidcProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
        AuthenticationManager oidcAuthManager = new ProviderManager(oidcProvider);
        authManagers.put(oidcIssuerUri, oidcAuthManager);
        logger.info("Registered OIDC authentication for issuer: {}", oidcIssuerUri);
      } catch (Exception e) {
        logger.warn(
            "Failed to initialize OIDC authentication for issuer '{}': {}. OIDC authentication will not be available.",
            oidcIssuerUri,
            e.getMessage());
      }
    } else {
      logger.info("OIDC authentication disabled (no issuer URI configured)");
    }
  }

  @Override
  public AuthenticationManager resolve(HttpServletRequest request) {
    final String authHeader = request.getHeader("Authorization");

    final String token = authHeader.substring(7);

    try {
      String issuer = jwtUtil.extractIssuer(token);
      logger.debug("Extracted issuer from token: '{}'", issuer);

      AuthenticationManager authManager = authManagers.get(issuer);
      return authManager != null ? authManager : defaultAuthManager;

    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to resolve AuthenticationManager: " + e.getMessage(), e);
    }
  }
}
