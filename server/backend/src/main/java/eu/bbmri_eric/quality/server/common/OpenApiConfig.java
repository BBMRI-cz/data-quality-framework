package eu.bbmri_eric.quality.server.common;

import eu.bbmri_eric.quality.server.setting.OidcDiscoveryDTO;
import eu.bbmri_eric.quality.server.setting.OidcDiscoveryService;
import eu.bbmri_eric.quality.server.setting.OidcIssuerProvider;
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
import io.swagger.v3.oas.models.servers.Server;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Open API docs. Uses lazy initialization via OpenApiCustomizer to avoid blocking
 * application startup with network calls.
 */
@Configuration
class OpenApiConfig {

  private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);

  private final OidcDiscoveryService oidcDiscoveryService;
  private final SettingService settingService;
  private final OidcIssuerProvider oidcIssuerProvider;

  public OpenApiConfig(
      OidcDiscoveryService oidcDiscoveryService,
      SettingService settingService,
      OidcIssuerProvider oidcIssuerProvider) {
    this.oidcDiscoveryService = oidcDiscoveryService;
    this.settingService = settingService;
    this.oidcIssuerProvider = oidcIssuerProvider;
  }

  @Bean
  OpenAPI baseOpenAPI() {
    final String securitySchemeName = "bearerAuth";
    return new OpenAPI()
        .addServersItem(new Server().url("/").description("Default Server URL"))
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
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  @Bean
  OpenApiCustomizer oauthSecurityCustomizer() {
    return openApi -> {
      try {
        configureOAuth2Security(openApi);
      } catch (Exception e) {
        log.warn("Could not configure OAuth2 for Swagger UI: {}", e.getMessage());
        log.debug("Full error details:", e);
      }
    };
  }

  private void configureOAuth2Security(OpenAPI openApi) {
    String oidcIssuerUri = oidcIssuerProvider.getIssuerUri();

    if (oidcIssuerUri == null || oidcIssuerUri.isBlank()) {
      log.warn(
          "OIDC issuer URI not configured, OAuth2 security scheme not configured for Swagger UI");
      return;
    }

    OidcDiscoveryDTO discovery = oidcDiscoveryService.fetchDiscoveryDocument(oidcIssuerUri);
    OidcSettingsDTO oidcSettings = settingService.getOidcSettings();

    if (discovery != null
        && discovery.getAuthorizationEndpoint() != null
        && discovery.getTokenEndpoint() != null
        && oidcSettings != null
        && oidcSettings.getOidcScopes() != null
        && !oidcSettings.getOidcScopes().isBlank()) {

      List<String> scopesList = Arrays.asList(oidcSettings.getOidcScopes().trim().split("\\s+"));
      log.info("Configuring OAuth2 for Swagger UI with scopes: {}", scopesList);

      Scopes oauthScopes = new Scopes();
      for (String scope : scopesList) {
        oauthScopes.addString(scope, "no description");
      }

      openApi
          .getComponents()
          .addSecuritySchemes(
              "oauth2",
              new SecurityScheme()
                  .type(SecurityScheme.Type.OAUTH2)
                  .flows(
                      new OAuthFlows()
                          .authorizationCode(
                              new OAuthFlow()
                                  .authorizationUrl(discovery.getAuthorizationEndpoint())
                                  .tokenUrl(discovery.getTokenEndpoint())
                                  .scopes(oauthScopes))));

      openApi.addSecurityItem(new SecurityRequirement().addList("oauth2", scopesList));
    } else {
      log.warn(
          "OIDC discovery or settings unavailable, OAuth2 security scheme not configured for Swagger UI");
    }
  }
}
