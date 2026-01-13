package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** DTO for assigning agents to a group. */
@Schema(name = "Assign Agents", description = "Data for assigning agents to a group")
public class AssignAgentsDTO {

  @NotNull(message = "Agent IDs cannot be null")
  @Schema(
      description = "List of agent IDs to assign to the group",
      example = "[\"agent-1\", \"agent-2\"]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<String> agentIds;

  public AssignAgentsDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param agentIds the list of agent IDs to assign
   */
  public AssignAgentsDTO(List<String> agentIds) {
    this.agentIds = agentIds;
  }

  public List<String> getAgentIds() {
    return agentIds;
  }

  public void setAgentIds(List<String> agentIds) {
    this.agentIds = agentIds;
  }
}
