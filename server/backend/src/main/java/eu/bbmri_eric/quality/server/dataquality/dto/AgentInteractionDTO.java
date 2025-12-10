package eu.bbmri_eric.quality.server.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.quality.server.dataquality.domain.AgentInteractionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.springframework.hateoas.server.core.Relation;

@Schema(description = "Agent interaction data transfer object")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(itemRelation = "interaction", collectionRelation = "interactions")
public class AgentInteractionDTO {

  @Schema(
      description = "Unique identifier of the interaction",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private String id;

  @Schema(description = "Timestamp of the interaction")
  private LocalDateTime timestamp;

  @Schema(description = "Type of the interaction", example = "REPORT")
  private AgentInteractionType type;

  public AgentInteractionDTO() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public AgentInteractionType getType() {
    return type;
  }

  public void setType(AgentInteractionType type) {
    this.type = type;
  }
}
