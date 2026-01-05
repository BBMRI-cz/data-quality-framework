package eu.bbmri_eric.quality.agent.server.dto;

import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/**
 * Data Transfer Object for basic Server information without interactions.
 *
 * <p>Contains basic server fields for list operations, excluding interactions and sensitive data to
 * improve performance and reduce response size.
 */
@Schema(description = "Basic server information")
@Relation(itemRelation = "server", collectionRelation = "servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServerDto {

  /** Unique identifier for the server. */
  @Schema(description = "Unique identifier for the server", example = "1")
  private String id;

  /** URL of the central server. */
  @Schema(description = "URL of the central server", example = "https://central.example.com")
  private String url;

  /** Display name for the server. */
  @Schema(description = "Display name for the server", example = "Production Central Server")
  private String name;

  /** Current status of the server connection. */
  @Schema(description = "Current status of the server connection", example = "ACTIVE")
  private ServerConnectionStatus status;

}
