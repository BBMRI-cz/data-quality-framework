package eu.bbmri_eric.quality.server.common;

import eu.bbmri_eric.quality.server.setting.OidcSettingsDTO;
import eu.bbmri_eric.quality.server.setting.SettingService;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Configuration class for Swagger UI OAuth settings. Dynamically sets the OAuth client ID and
 * scopes from database settings.
 */
@Configuration
class SwaggerUiConfig {

  private static final Logger log = LoggerFactory.getLogger(SwaggerUiConfig.class);
  private static final String DEFAULT_CLIENT_ID = "auth-code-client";
  private static final String DEFAULT_SCOPES = "openid profile email permissions";

  private final SettingService settingService;
  private final ConfigurableEnvironment environment;

  public SwaggerUiConfig(SettingService settingService, ConfigurableEnvironment environment) {
    this.settingService = settingService;
    this.environment = environment;
  }

  @PostConstruct
  public void configureSwaggerUiOAuth() {
    try {
      OidcSettingsDTO oidcSettings = settingService.getOidcSettings();
      String clientId = DEFAULT_CLIENT_ID;
      String scopes = DEFAULT_SCOPES;

      if (oidcSettings.getOidcClientId() != null && !oidcSettings.getOidcClientId().isBlank()) {
        clientId = oidcSettings.getOidcClientId();
      } else {
        log.debug("Using default Swagger UI OAuth client ID: {}", clientId);
      }

      if (oidcSettings.getOidcScopes() != null && !oidcSettings.getOidcScopes().isBlank()) {
        scopes = oidcSettings.getOidcScopes();
      } else {
        log.debug("Using default Swagger UI OAuth scopes: {}", scopes);
      }

      Map<String, Object> swaggerProps = new HashMap<>();
      swaggerProps.put("springdoc.swagger-ui.oauth.client-id", clientId);
      swaggerProps.put("springdoc.swagger-ui.oauth.scopes", scopes);

      environment
          .getPropertySources()
          .addFirst(new MapPropertySource("swaggerUiDynamicConfig", swaggerProps));

    } catch (Exception e) {
      log.warn(
          "Could not fetch OIDC settings from database, using defaults (client-id: {}, scopes: {}). Error: {}",
          DEFAULT_CLIENT_ID,
          DEFAULT_SCOPES,
          e.getMessage());
      log.debug("Full error details:", e);
    }
  }
}
