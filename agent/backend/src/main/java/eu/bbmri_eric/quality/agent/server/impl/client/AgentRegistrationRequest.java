package eu.bbmri_eric.quality.agent.server.impl.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Data Transfer Object for agent registration requests to central servers. */
@Schema(description = "Agent registration request")
class AgentRegistrationRequest {

  /** Agent ID that must be a valid UUID. */
  @NotBlank(message = "Agent ID cannot be blank")
  @Pattern(
      regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
      message = "Agent ID must be a valid UUID format")
  @Schema(
      description = "Unique identifier for the agent",
      example = "123e4567-e89b-12d3-a456-426614174000",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  private String version = "unknown";

  /** Default constructor. */
  public AgentRegistrationRequest() {}

  /**
   * Constructor with agent ID.
   *
   * @param id the agent ID
   */
  public AgentRegistrationRequest(String id) {
    this.id = id;
  }

  public AgentRegistrationRequest(String id, String version) {
    this.id = id;
    this.version = version;
  }

  /**
   * Gets the agent ID.
   *
   * @return the agent ID
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the agent ID.
   *
   * @param id the agent ID
   */
  public void setId(String id) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
