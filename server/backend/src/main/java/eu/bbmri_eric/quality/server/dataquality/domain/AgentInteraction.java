package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an interaction with an agent.
 *
 * <p>Each interaction is automatically assigned a unique identifier and timestamp upon creation.
 * Interactions are associated with an agent and have a type (e.g., REPORT, PING).
 */
@Entity(name = "agent_interaction")
public class AgentInteraction {
  @Id private final String id = UUID.randomUUID().toString();

  private final LocalDateTime timestamp = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  private AgentInteractionType type;

  @Column(name = "agent_id", insertable = false, updatable = false)
  private String agentId;

  /** Default constructor for JPA. */
  protected AgentInteraction() {}

  /**
   * Creates a new agent interaction.
   *
   * @param agentId the ID of the agent associated with this interaction
   * @param type the type of interaction
   * @throws IllegalArgumentException if agentId or type is null
   */
  AgentInteraction(String agentId, AgentInteractionType type) {
    if (agentId == null || agentId.trim().isEmpty()) {
      throw new IllegalArgumentException("Agent ID cannot be null or empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("Interaction type cannot be null");
    }
    this.agentId = agentId;
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public AgentInteractionType getType() {
    return type;
  }

  public void setType(AgentInteractionType type) {
    this.type = type;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    AgentInteraction that = (AgentInteraction) o;
    return Objects.equals(id, that.id)
        && Objects.equals(timestamp, that.timestamp)
        && type == that.type
        && Objects.equals(agentId, that.agentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, timestamp, type, agentId);
  }
}
