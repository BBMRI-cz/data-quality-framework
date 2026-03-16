package eu.bbmri_eric.quality.server.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.quality.server.dataquality.domain.AgentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.server.core.Relation;

@Schema(description = "Agent data transfer object")
@Relation(itemRelation = "agent", collectionRelation = "agents")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentDTO {

  @Schema(
      description = "Unique identifier of the agent",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private String id;

  @Schema(
      description = "Current status of the agent",
      example = "ACTIVE",
      allowableValues = {"ACTIVE", "INACTIVE", "PENDING"})
  private AgentStatus status;

  @Schema(description = "Human-readable name of the agent", example = "Hospital Alpha Data Center")
  private String name;

  @Schema(description = "Version of the agent software", example = "1.0.0")
  private String version;

  @Schema(description = "External identifier for the agent", example = "BIOBANK-001")
  private String externalIdentifier;

  @Schema(description = "List of agent interactions (only included when expand=interactions)")
  private List<AgentInteractionDTO> interactions;

  @Schema(description = "List of groups this agent belongs to")
  private List<GroupDTO> groups;

  public AgentDTO() {}

  public List<GroupDTO> getGroups() {
    return groups;
  }

  public void setGroups(List<GroupDTO> groups) {
    this.groups = groups;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public AgentStatus getStatus() {
    return status;
  }

  public void setStatus(AgentStatus status) {
    this.status = status;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getExternalIdentifier() {
    return externalIdentifier;
  }

  public void setExternalIdentifier(String externalIdentifier) {
    this.externalIdentifier = externalIdentifier;
  }

  public List<AgentInteractionDTO> getInteractions() {
    return interactions;
  }

  public void setInteractions(List<AgentInteractionDTO> interactions) {
    this.interactions = interactions;
  }
}
