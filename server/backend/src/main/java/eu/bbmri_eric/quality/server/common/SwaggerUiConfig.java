package eu.bbmri_eric.quality.server.common;

import eu.bbmri_eric.quality.server.setting.OidcSettingsDTO;
import eu.bbmri_eric.quality.server.setting.SettingService;
import java.util.HashMap;
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

  SwaggerUiConfig(SettingService settingService, ConfigurableEnvironment environment) {

    environment.getPropertySources().addFirst(new SwaggerUiLazyPropertySource(settingService));
  }

  /**
   * Lazy PropertySource that fetches OIDC settings only when Swagger UI OAuth properties are
   * requested.
   */
  static class SwaggerUiLazyPropertySource extends MapPropertySource {

    private final SettingService settingService;

    SwaggerUiLazyPropertySource(SettingService settingService) {
      super("swaggerUiDynamicConfig", new HashMap<>());
      this.settingService = settingService;
    }

    @Override
    public Object getProperty(String name) {

      if (!name.startsWith("springdoc.swagger-ui.oauth.")) {
        return null;
      }

      try {
        OidcSettingsDTO settings = settingService.getOidcSettings();

        if ("springdoc.swagger-ui.oauth.client-id".equals(name)) {
          String clientId = settings.getOidcClientId();
          if (clientId != null && !clientId.isBlank()) {
            return clientId;
          }
          return null;
        }

        if ("springdoc.swagger-ui.oauth.scopes".equals(name)) {
          String scopes = settings.getOidcScopes();
          if (scopes != null && !scopes.isBlank()) {
            return scopes;
          }
          return null;
        }

      } catch (Exception e) {
        log.warn("Could not fetch OIDC settings from database. Error: {}", e.getMessage());
        log.debug("Full error details:", e);
      }

      return null;
    }
  }
}
