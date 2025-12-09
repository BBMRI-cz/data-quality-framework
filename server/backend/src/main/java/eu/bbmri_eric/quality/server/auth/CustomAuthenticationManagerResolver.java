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
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Selects the appropriate AuthenticationManager based on the JWT token's issuer claim in the
 * incoming JWT bearer token. Supports Internal application tokens and OIDC provider tokens.
 */
public class CustomAuthenticationManagerResolver
    implements AuthenticationManagerResolver<HttpServletRequest> {

  private static final Logger logger =
      LoggerFactory.getLogger(CustomAuthenticationManagerResolver.class);
  private static final String INTERNAL_ISSUER = "quality-server";

  private final JwtUtil jwtUtil;
  private final Map<String, AuthenticationManager> authManagers;
  private final AuthenticationManager defaultAuthManager;
  private final BearerTokenResolver bearerTokenResolver;

  public CustomAuthenticationManagerResolver(
      JwtUtil jwtUtil,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      UserDetailsService userDetailsService,
      String oidcIssuerUri) {
    this.jwtUtil = jwtUtil;
    this.authManagers = new HashMap<>();
    this.bearerTokenResolver = new DefaultBearerTokenResolver();

    InternalTokenAuthenticationProvider internalProvider =
        new InternalTokenAuthenticationProvider(jwtUtil, userDetailsService);
    AuthenticationManager internalAuthManager = new ProviderManager(internalProvider);
    authManagers.put(INTERNAL_ISSUER, internalAuthManager);
    this.defaultAuthManager = internalAuthManager;

    if (oidcIssuerUri != null && !oidcIssuerUri.isBlank()) {
      try {
        JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(oidcIssuerUri);
        JwtAuthenticationProvider oidcProvider = new JwtAuthenticationProvider(jwtDecoder);
        oidcProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
        AuthenticationManager oidcAuthManager = new ProviderManager(oidcProvider);
        authManagers.put(oidcIssuerUri, oidcAuthManager);
        logger.debug("Registered OIDC authentication for issuer: {}", oidcIssuerUri);
      } catch (Exception e) {
        logger.warn(
            "Failed to initialize OIDC authentication for issuer '{}': {}. OIDC authentication will not be available.",
            oidcIssuerUri,
            e.getMessage());
      }
    } else {
      logger.warn("OIDC authentication disabled (no issuer URI configured)");
    }
  }

  @Override
  public AuthenticationManager resolve(HttpServletRequest request) {
    String token = bearerTokenResolver.resolve(request);

    if (token == null) {
      logger.warn("No bearer token found in request from IP: {}", request.getRemoteAddr());
      throw new IllegalStateException("No bearer token found in request");
    }

    try {
      String issuer = jwtUtil.extractIssuer(token);
      logger.debug("Extracted issuer from token: '{}'", issuer);

      AuthenticationManager authManager = authManagers.get(issuer);
      return authManager != null ? authManager : defaultAuthManager;
    } catch (IllegalArgumentException e) {
      return defaultAuthManager;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to resolve AuthenticationManager: " + e.getMessage(), e);
    }
  }
}
