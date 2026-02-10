package eu.bbmri_eric.quality.server.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for customizing actuator endpoints in OpenAPI documentation. Marks actuator
 * endpoints (health, info) as not requiring authentication.
 */
@Configuration
public class ActuatorOpenApiConfig {

  String[] actuatorPaths = {"/api/health", "/api/info", "/api", "/api/counts"};

  /**
   * Customizes OpenAPI documentation for actuator endpoints by removing security requirements. This
   * ensures that /api/health and /api/info are documented as publicly accessible.
   */
  @Bean
  public OpenApiCustomizer actuatorOpenApiCustomizer() {
    return openApi -> {
      if (openApi.getPaths() != null) {
        for (String path : actuatorPaths) {
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
