package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO for updating category data. */
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

  /** Default constructor for serialization frameworks. */
  public CategoryUpdateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the category
   * @param colorHex the hex color code for visual representation
   */
  public CategoryUpdateDTO(String name, String colorHex) {
    this.name = name;
    this.colorHex = colorHex;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getColorHex() {
    return colorHex;
  }

  public void setColorHex(String colorHex) {
    this.colorHex = colorHex;
  }
}
