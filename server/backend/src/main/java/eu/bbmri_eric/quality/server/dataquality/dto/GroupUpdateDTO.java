package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO for updating a group. */
@Schema(name = "Group Update", description = "Data for updating a group")
public class GroupUpdateDTO {

  @NotBlank(message = "Name cannot be blank")
  @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
  @Schema(
      description = "Name of the group",
      example = "Production Agents",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  public GroupUpdateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the group
   */
  public GroupUpdateDTO(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
