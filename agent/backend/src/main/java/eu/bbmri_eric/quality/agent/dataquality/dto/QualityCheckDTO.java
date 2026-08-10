package eu.bbmri_eric.quality.agent.dataquality.dto;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheckType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/** DTO for Quality Check entity. */
@Setter
@Getter
@Schema(name = "Quality Check", description = "A data quality check")
@Relation(itemRelation = "quality-check", collectionRelation = "quality-checks")
public class QualityCheckDTO {

  @Schema(description = "Unique identifier of the quality check", example = "1")
  private Long id;

  @Schema(description = "Name of the quality check", example = "Patient Age Validation")
  private String name;

  @Schema(
      description = "Description of what the check validates",
      example = "Validates patient ages are within acceptable range")
  private String description;

  @Schema(description = "Query string", example = "library PatientAgeValidation version '1.0.0'...")
  private String query;

  @Schema(description = "Type of quality check", example = "CQL")
  private QualityCheckType type;

  @Schema(description = "Warning threshold percentage", example = "10")
  private int warningThreshold;

  @Schema(description = "Error threshold percentage", example = "30")
  private int errorThreshold;

  @Schema(description = "Epsilon budget for differential privacy", example = "1.0")
  private Double epsilonBudget;

  @Schema(description = "Category for grouping quality checks")
  private CategoryDTO category;

  public QualityCheckDTO() {}

  public QualityCheckDTO(
      Long id,
      String name,
      String description,
      String query,
      QualityCheckType type,
      int warningThreshold,
      int errorThreshold,
      double epsilonBudget) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.query = query;
    this.type = type;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.epsilonBudget = epsilonBudget;
  }

  public QualityCheckDTO(
      Long id,
      String name,
      String description,
      String query,
      QualityCheckType type,
      int warningThreshold,
      int errorThreshold,
      double epsilonBudget,
      CategoryDTO category) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.query = query;
    this.type = type;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.epsilonBudget = epsilonBudget;
    this.category = category;
  }
}
