package eu.bbmri_eric.quality.agent.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for Open API docs */
@Configuration
class OpenApiConfig {

  @Bean
  OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Data Quality Agent API")
                .description(
                    "This API exposes Spring Data REST endpoints for managing members and other resources.")
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
                    "basicAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
        .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
  }
}
