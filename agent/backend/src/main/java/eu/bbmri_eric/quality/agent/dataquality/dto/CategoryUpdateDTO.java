package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** DTO for updating category data. */
@Setter
@Getter
@Schema(name = "Category Update", description = "Data for updating a category")
public class CategoryUpdateDTO {

  @NotBlank(message = "Name cannot be blank")
  @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
  @Schema(description = "Name of the category", example = "Data Completeness")
  private String name;

  @Pattern(
      regexp = "^#[0-9A-Fa-f]{6}$",
      message = "Color must be a valid hex color code (e.g., #FF5733)")
  @Schema(
      description = "Hex color code for visual representation",
      example = "#FF5733",
      pattern = "^#[0-9A-Fa-f]{6}$")
  private String colorHex;

  public CategoryUpdateDTO() {}

  public CategoryUpdateDTO(String name, String colorHex) {
    this.name = name;
    this.colorHex = colorHex;
  }
}
