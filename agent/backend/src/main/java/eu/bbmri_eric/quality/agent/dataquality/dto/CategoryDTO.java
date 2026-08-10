package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/** DTO for category data. */
@Setter
@Getter
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

  @Schema(description = "Hex color code for visual representation", example = "#FF5733")
  private String colorHex;

  public CategoryDTO() {}

  public CategoryDTO(Long id, String name, String colorHex) {
    this.id = id;
    this.name = name;
    this.colorHex = colorHex;
  }
}
