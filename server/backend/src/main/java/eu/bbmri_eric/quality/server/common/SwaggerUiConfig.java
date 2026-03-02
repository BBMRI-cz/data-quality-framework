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

      if (!name.startsWith("springdoc.swagger-ui.oauth")) {
        return null;
      }

      try {
        OidcSettingsDTO settings = settingService.getOidcSettings();

        switch (name) {
          case "springdoc.swagger-ui.oauth.client-id" -> {
            String clientId = settings.getOidcClientId();
            if (clientId != null && !clientId.isBlank()) {
              return clientId;
            }
            return null;
          }
          case "springdoc.swagger-ui.oauth.scopes" -> {
            String scopes = settings.getOidcScopes();
            if (scopes != null && !scopes.isBlank()) {
              return scopes;
            }
            return null;
          }
          case "springdoc.swagger-ui.oauth2-redirect-url" -> {
            String redirectUrl = settings.getOidcSwaggerRedirectUrl();
            log.info("SwaggerUiConfig: Returning oauth2-redirect-url: {}", redirectUrl);
            if (redirectUrl != null && !redirectUrl.isBlank()) {
              return redirectUrl;
            }
            log.info("SwaggerUiConfig: oauth2-redirect-url is null or blank, returning null");
            return null;
          }
        }

      } catch (Exception e) {
        log.warn("Could not fetch OIDC settings from database. Error: {}", e.getMessage());
        log.debug("Full error details:", e);
      }

      return null;
    }
  }
}
