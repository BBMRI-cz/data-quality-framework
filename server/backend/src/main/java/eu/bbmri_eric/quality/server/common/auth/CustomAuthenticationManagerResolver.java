package eu.bbmri_eric.quality.server.common.auth;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import eu.bbmri_eric.quality.server.setting.OidcIssuerProvider;
import eu.bbmri_eric.quality.server.setting.OidcSettingsUpdatedEvent;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Selects the appropriate AuthenticationManager based on the JWT token's issuer claim in the
 * incoming JWT bearer token. Supports Internal application tokens and OIDC provider tokens.
 */
@Component
class CustomAuthenticationManagerResolver
    implements AuthenticationManagerResolver<HttpServletRequest> {

  private static final Logger logger =
      LoggerFactory.getLogger(CustomAuthenticationManagerResolver.class);
  private static final String INTERNAL_ISSUER = "quality-server";

  private final JwtUtil jwtUtil;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;
  private final OidcIssuerProvider oidcIssuerProvider;
  private final Map<String, AuthenticationManager> authManagers;
  private final AuthenticationManager defaultAuthManager;
  private final BearerTokenResolver bearerTokenResolver;
  private volatile boolean oidcInitializationAttempted = false;
  private volatile String currentOidcIssuer = null;

  CustomAuthenticationManagerResolver(
      JwtUtil jwtUtil,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      UserDetailsService userDetailsService,
      OidcIssuerProvider oidcIssuerProvider) {
    this.jwtUtil = jwtUtil;
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    this.oidcIssuerProvider = oidcIssuerProvider;
    this.authManagers = new ConcurrentHashMap<>();
    this.bearerTokenResolver = new DefaultBearerTokenResolver();

    InternalTokenAuthenticationProvider internalProvider =
        new InternalTokenAuthenticationProvider(jwtUtil, userDetailsService);
    AuthenticationManager internalAuthManager = new ProviderManager(internalProvider);
    authManagers.put(INTERNAL_ISSUER, internalAuthManager);
    this.defaultAuthManager = internalAuthManager;

    logger.info(
        "Internal authentication manager initialized. OIDC will be initialized on first use.");
  }

  private void initializeOidcAuthentication() {
    if (oidcInitializationAttempted) {
      return;
    }

    synchronized (this) {
      if (oidcInitializationAttempted) {
        return;
      }

      try {
        String oidcIssuerUri = oidcIssuerProvider.getIssuerUri();

        if (oidcIssuerUri != null && !oidcIssuerUri.isBlank()) {
          NimbusJwtDecoder jwtDecoder =
              NimbusJwtDecoder.withIssuerLocation(oidcIssuerUri)
                  .jwtProcessorCustomizer(
                      processor ->
                          processor.setJWSTypeVerifier(
                              new DefaultJOSEObjectTypeVerifier<>(
                                  new JOSEObjectType("at+jwt"), new JOSEObjectType("JWT"), null)))
                  .build();
          JwtAuthenticationProvider oidcProvider = new JwtAuthenticationProvider(jwtDecoder);
          oidcProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
          AuthenticationManager oidcAuthManager = new ProviderManager(oidcProvider);
          authManagers.put(oidcIssuerUri.replaceAll("/+$", ""), oidcAuthManager);
          currentOidcIssuer = oidcIssuerUri.replaceAll("/+$", "");
          oidcInitializationAttempted = true;
          logger.info("Registered OIDC authentication for issuer: {}", oidcIssuerUri);
        } else {
          oidcInitializationAttempted = true;
          logger.info("OIDC authentication disabled (no issuer URI configured)");
        }
      } catch (Exception e) {
        logger.error(
            "Failed to initialize OIDC authentication: {}. Will retry on next request.",
            e.getMessage());
      }
    }
  }

  /**
   * Reinitializes OIDC authentication with the current issuer URI. This method is called
   * automatically when OIDC settings are updated in the database via event listener.
   */
  @EventListener(OidcSettingsUpdatedEvent.class)
  public void reinitializeOidcAuthentication() {
    synchronized (this) {
      logger.info("Received OIDC settings update event, reinitializing authentication");

      if (currentOidcIssuer != null) {
        authManagers.remove(currentOidcIssuer);
        logger.info("Removed cached OIDC authentication manager for issuer: {}", currentOidcIssuer);
      }

      oidcInitializationAttempted = false;
      currentOidcIssuer = null;

      initializeOidcAuthentication();
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

      if (!INTERNAL_ISSUER.equals(issuer) && !oidcInitializationAttempted) {
        initializeOidcAuthentication();
      }

      AuthenticationManager authManager = authManagers.get(issuer);
      return authManager != null ? authManager : defaultAuthManager;
    } catch (IllegalArgumentException e) {
      return defaultAuthManager;
    } catch (JwtException e) {
      throw new AuthenticationServiceException(e.getMessage());
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to resolve AuthenticationManager: " + e.getMessage(), e);
    }
  }

  private static @NonNull OAuth2TokenValidator<Jwt> getJwtOAuth2TokenValidator() {
    return jwt -> {
      Object typObj = jwt.getHeaders().get("typ");
      if (typObj == null) {
        return OAuth2TokenValidatorResult.success();
      }
      String typ = typObj.toString();
      if ("JWT".equalsIgnoreCase(typ) || "at+jwt".equalsIgnoreCase(typ)) {
        return OAuth2TokenValidatorResult.success();
      }
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Unsupported JOSE typ: " + typ, null));
    };
  }
}
