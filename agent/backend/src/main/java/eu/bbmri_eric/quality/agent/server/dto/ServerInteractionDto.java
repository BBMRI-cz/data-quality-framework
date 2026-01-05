package eu.bbmri_eric.quality.agent.server.dto;

import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/**
 * Data Transfer Object for ServerInteraction information.
 *
 * <p>Contains interaction details that should be exposed to the client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Server interaction log entry")
@Relation(itemRelation = "server-interaction", collectionRelation = "server-interactions")
public class ServerInteractionDto {

  /** Unique identifier for the interaction. */
  @Schema(description = "Unique identifier for the interaction", example = "1")
  private Long id;

  /** Type of interaction. */
  @Schema(description = "Type of interaction", example = "UPDATE")
  private InteractionType type;

  /** Description of what happened in this interaction. */
  @Schema(
      description = "Description of what happened in this interaction",
      example =
          "Server created with name='Production Central Server', url='https://central.example.com'")
  private String description;

  /** Timestamp when the interaction occurred. */
  @Schema(description = "Timestamp when the interaction occurred", example = "2025-10-21T14:30:00")
  private LocalDateTime timestamp;

}
