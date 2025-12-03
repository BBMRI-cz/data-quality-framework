package eu.bbmri_eric.quality.agent.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a log entry of an interaction with a central server.
 *
 * <p>This entity tracks all operations performed on or with a server, including configuration
 * updates and communication attempts. Each interaction is timestamped and categorized by type.
 */
@Getter
@Setter
@Entity
public class ServerInteraction {

  /**
   * Auto-generated unique identifier for the interaction. -- GETTER -- Gets the unique identifier
   * of the interaction.
   *
   * @return the interaction ID
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Type of interaction performed. -- GETTER -- Gets the type of interaction.
   *
   * @return the interaction type
   */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private InteractionType type;

  /**
   * Detailed description of the interaction. -- GETTER -- Gets the description of the interaction.
   *
   * @return the interaction description
   */
  @NotBlank
  @Column(name = "description", nullable = false)
  private String description;

  /**
   * Timestamp when the interaction occurred. -- GETTER -- Gets the timestamp when the interaction
   * occurred.
   *
   * @return the interaction timestamp
   */
  @NotNull
  @Column(name = "timestamp")
  private final LocalDateTime timestamp = LocalDateTime.now();

  /**
   * The ID of the server this interaction belongs to. -- GETTER -- Gets the ID of the server this
   * interaction belongs to.
   *
   * @return the server ID
   */
  @NotNull
  @Column(nullable = false)
  private String serverId;

  /** Default constructor required by JPA. */
  protected ServerInteraction() {}

  /**
   * Constructs a new ServerInteraction with the specified parameters.
   *
   * <p>The timestamp will be automatically set to the current time when the entity is persisted.
   *
   * @param type the type of interaction
   * @param description the detailed description of the interaction
   */
  public ServerInteraction(InteractionType type, String description) {
    this.type = type;
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ServerInteraction that = (ServerInteraction) o;
    return Objects.equals(id, that.id)
        && type == that.type
        && Objects.equals(description, that.description)
        && Objects.equals(timestamp, that.timestamp)
        && Objects.equals(serverId, that.serverId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, description, timestamp, serverId);
  }

  @Override
  public String toString() {
    return "ServerInteraction{"
        + "id="
        + id
        + ", type="
        + type
        + ", description='"
        + description
        + '\''
        + ", timestamp="
        + timestamp
        + '}';
  }
}
