package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

/** DTO for category data. */
@Schema(name = "Category", description = "A category for grouping quality checks")
@Relation(itemRelation = "category", collectionRelation = "categories")
public class CategoryDTO {

  @Schema(
      description = "Unique identifier of the category",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(
      description = "Name of the category",
      example = "Data Completeness",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(
      description = "Hex color code for visual representation",
      example = "#FF5733")
  private String colorHex;

  /** Default constructor for serialization frameworks. */
  public CategoryDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param id the unique identifier of the category
   * @param name the name of the category
   * @param colorHex the hex color code for visual representation
   */
  public CategoryDTO(Long id, String name, String colorHex) {
    this.id = id;
    this.name = name;
    this.colorHex = colorHex;
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

  public String getColorHex() {
    return colorHex;
  }

  public void setColorHex(String colorHex) {
    this.colorHex = colorHex;
  }
}


