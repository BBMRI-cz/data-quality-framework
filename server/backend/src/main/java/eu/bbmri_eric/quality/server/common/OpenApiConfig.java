package eu.bbmri_eric.quality.server.common;

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
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for Open API docs */
@Configuration
class OpenApiConfig {

  private final List<String> scopes =
      List.of("openid", "profile", "email", "permissions", "some-app-scope-1");

  @Bean
  OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";
    Scopes oauthScopes = new Scopes();
    for (String scope : scopes) {
      oauthScopes.addString(scope, "no description");
    }

    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth").addList("oauth2", scopes))
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
                                        .authorizationUrl("http://localhost:4011/connect/authorize")
                                        .tokenUrl("http://localhost:4011/connect/token")
                                        .scopes(oauthScopes)))));
  }
}
