package eu.bbmri_eric.quality.agent.dataquality.dto;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheckType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** DTO for updating an existing Quality Check. */
@Setter
@Getter
@Schema(name = "Quality Check Update", description = "Data for updating an existing quality check")
public class QualityCheckUpdateDTO {

  @Size(max = 255, message = "Name must not exceed 255 characters")
  @Schema(description = "Name of the quality check", example = "Patient Age Validation")
  private String name;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  @Schema(
      description = "Description of what the check validates",
      example = "Validates patient ages are within acceptable range")
  private String description;

  @Schema(description = "Query string", example = "library PatientAgeValidation version '1.0.0'...")
  private String query;

  @Schema(description = "Type of quality check", example = "CQL")
  private QualityCheckType type;

  @Min(value = 0, message = "Warning threshold must be at least 0")
  @Max(value = 100, message = "Warning threshold must not exceed 100")
  @Schema(description = "Warning threshold percentage", example = "10")
  private Integer warningThreshold;

  @Min(value = 0, message = "Error threshold must be at least 0")
  @Max(value = 100, message = "Error threshold must not exceed 100")
  @Schema(description = "Error threshold percentage", example = "30")
  private Integer errorThreshold;

  @Positive(message = "Epsilon budget must be positive")
  @Schema(description = "Epsilon budget for differential privacy", example = "1.0")
  private Double epsilonBudget;

  public QualityCheckUpdateDTO() {}

  public QualityCheckUpdateDTO(
      String name,
      String description,
      String query,
      QualityCheckType type,
      Integer warningThreshold,
      Integer errorThreshold,
      Double epsilonBudget) {
    this.name = name;
    this.description = description;
    this.query = query;
    this.type = type;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.epsilonBudget = epsilonBudget;
  }
}
