package eu.bbmri_eric.quality.server.common;

import eu.bbmri_eric.quality.server.setting.OidcDiscoveryDTO;
import eu.bbmri_eric.quality.server.setting.OidcDiscoveryService;
import eu.bbmri_eric.quality.server.setting.OidcSettingsDTO;
import eu.bbmri_eric.quality.server.setting.SettingService;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for Open API docs */
@Configuration
class OpenApiConfig {

  private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);
  private static final List<String> DEFAULT_SCOPES =
      List.of("openid", "profile", "email", "permissions");

  private final OidcDiscoveryService oidcDiscoveryService;
  private final SettingService settingService;

  public OpenApiConfig(OidcDiscoveryService oidcDiscoveryService, SettingService settingService) {
    this.oidcDiscoveryService = oidcDiscoveryService;
    this.settingService = settingService;
  }

  @Bean
  OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    List<String> scopesList = DEFAULT_SCOPES;
    try {
      OidcSettingsDTO oidcSettings = settingService.getOidcSettings();
      if (oidcSettings.getOidcScopes() != null && !oidcSettings.getOidcScopes().isBlank()) {
        scopesList = Arrays.asList(oidcSettings.getOidcScopes().trim().split("\\s+"));
        log.debug("Using OIDC scopes from settings: {}", scopesList);
      } else {
        log.debug("Using default OIDC scopes: {}", scopesList);
      }
    } catch (Exception e) {
      log.debug("Could not fetch OIDC scopes from settings, using defaults: {}", e.getMessage());
    }

    Scopes oauthScopes = new Scopes();
    for (String scope : scopesList) {
      oauthScopes.addString(scope, "no description");
    }

    String authUrl = "http://localhost:4011/connect/authorize";
    String tokenUrl = "http://localhost:4011/connect/token";

    OidcDiscoveryDTO discovery = oidcDiscoveryService.fetchDiscoveryDocument();
    if (discovery != null) {
      if (discovery.getAuthorizationEndpoint() != null) {
        authUrl = discovery.getAuthorizationEndpoint();
      }
      if (discovery.getTokenEndpoint() != null) {
        tokenUrl = discovery.getTokenEndpoint();
      }
    } else {
      log.warn(
          "OIDC discovery unavailable, using default endpoints. "
              + "Swagger UI will use: authUrl={}, tokenUrl={}",
          authUrl,
          tokenUrl);
    }

    return new OpenAPI()
        .addSecurityItem(
            new SecurityRequirement().addList("bearerAuth").addList("oauth2", scopesList))
        .info(
            new Info()
                .title("Data Quality Server REST API")
                .version("1.0.0")
                .contact(
                    new Contact()
                        .name("BBMRI-ERIC")
                        .email("contact@bbmri-eric.eu")
                        .url("https://bbmri-eric.eu"))
                .license(
                    new License()
                        .name("GNU AFFERO GENERAL PUBLIC LICENSE")
                        .url("https://www.gnu.org/licenses/agpl-3.0.en.html")))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(
                    "oauth2",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(
                            new OAuthFlows()
                                .authorizationCode(
                                    new OAuthFlow()
                                        .authorizationUrl(authUrl)
                                        .tokenUrl(tokenUrl)
                                        .scopes(oauthScopes)))));
  }
}
