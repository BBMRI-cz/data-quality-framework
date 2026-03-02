package eu.bbmri_eric.quality.server.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for customizing actuator endpoints in OpenAPI documentation. Marks actuator
 * endpoints (health, info, counts) as not requiring authentication.
 */
@Configuration
public class ActuatorOpenApiConfig {

  private static final List<String> ACTUATOR_PATHS =
      List.of("/api/health", "/api/info", "/api/counts");

  /** Customizes OpenAPI documentation for actuator endpoints by removing security requirements. */
  @Bean
  public OpenApiCustomizer actuatorOpenApiCustomizer() {
    return openApi -> {
      if (openApi.getPaths() != null) {
        for (String path : ACTUATOR_PATHS) {
          customizeEndpoint(openApi, path);
        }
      }
    };
  }

  private void customizeEndpoint(OpenAPI openApi, String path) {
    PathItem pathItem = openApi.getPaths().get(path);
    if (pathItem != null) {
      clearSecurityRequirements(pathItem.getGet());
    }
  }

  private void clearSecurityRequirements(Operation operation) {
    if (operation != null) {
      operation.setSecurity(java.util.Collections.emptyList());
    }
  }
}
