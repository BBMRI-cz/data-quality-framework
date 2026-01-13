package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO for creating a new group. */
@Schema(name = "Group Create", description = "Data for creating a new group")
public class GroupCreateDTO {

  @NotBlank(message = "Name cannot be blank")
  @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
  @Schema(
      description = "Name of the group",
      example = "Production Agents",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  public GroupCreateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the group
   */
  public GroupCreateDTO(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
