package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.server.core.Relation;

/** DTO for group data. */
@Schema(name = "Group", description = "A group for organizing agents")
@Relation(itemRelation = "group", collectionRelation = "groups")
public class GroupDTO {

  @Schema(
      description = "Unique identifier of the group",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(
      description = "Name of the group",
      example = "Production Agents",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "List of agent IDs in this group")
  private List<String> agentIds;

  public GroupDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param id the unique identifier of the group
   * @param name the name of the group
   * @param agentIds the list of agent IDs in this group
   */
  public GroupDTO(Long id, String name, List<String> agentIds) {
    this.id = id;
    this.name = name;
    this.agentIds = agentIds;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getAgentIds() {
    return agentIds;
  }

  public void setAgentIds(List<String> agentIds) {
    this.agentIds = agentIds;
  }
}
