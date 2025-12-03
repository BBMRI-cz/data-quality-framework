package eu.bbmri_eric.quality.agent.server.dto;

import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.springframework.hateoas.server.core.Relation;

/**
 * Data Transfer Object for ServerInteraction information.
 *
 * <p>Contains interaction details that should be exposed to the client.
 */
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

  /** Default constructor. */
  public ServerInteractionDto() {}

  /**
   * Constructor with all fields.
   *
   * @param id the interaction ID
   * @param type the interaction type
   * @param description the interaction description
   * @param timestamp the interaction timestamp
   */
  public ServerInteractionDto(
      Long id, InteractionType type, String description, LocalDateTime timestamp) {
    this.id = id;
    this.type = type;
    this.description = description;
    this.timestamp = timestamp;
  }

  /**
   * Gets the ID.
   *
   * @return the ID
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the ID.
   *
   * @param id the ID
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public InteractionType getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the type
   */
  public void setType(InteractionType type) {
    this.type = type;
  }

  /**
   * Gets the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the timestamp.
   *
   * @return the timestamp
   */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the timestamp.
   *
   * @param timestamp the timestamp
   */
  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }
}
