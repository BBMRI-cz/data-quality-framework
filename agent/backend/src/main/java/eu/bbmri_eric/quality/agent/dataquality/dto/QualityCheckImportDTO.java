package eu.bbmri_eric.quality.agent.dataquality.dto;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * One entry of a quality check import file. The field names match the export format, so a file
 * produced by the export endpoint can be imported unchanged.
 */
@Setter
@Getter
@Schema(
    name = "Quality Check Import",
    description = "A quality check entry in the portable import file format")
public class QualityCheckImportDTO {

  @NotBlank(message = "Name is required")
  @Size(max = 255, message = "Name must not exceed 255 characters")
  @Schema(description = "Name of the quality check", example = "Patient Age Validation")
  private String name;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  @Schema(
      description = "Description of what the check validates",
      example = "Validates patient ages are within acceptable range")
  private String description;

  @Schema(
      description =
          "Query string; required for every type except JAVA, whose query is implemented in the"
              + " agent and therefore ignored here",
      example = "SELECT count(*) FROM patient")
  private String query;

  @Schema(description = "Type of quality check, defaults to CQL when absent", example = "CQL")
  private QualityCheckType type;

  @NotNull(message = "Warning threshold is required")
  @Min(value = 0, message = "Warning threshold must be at least 0")
  @Max(value = 100, message = "Warning threshold must not exceed 100")
  @Schema(description = "Warning threshold percentage", example = "10")
  private Integer warningThreshold;

  @NotNull(message = "Error threshold is required")
  @Min(value = 0, message = "Error threshold must be at least 0")
  @Max(value = 100, message = "Error threshold must not exceed 100")
  @Schema(description = "Error threshold percentage", example = "30")
  private Integer errorThreshold;

  @Positive(message = "Epsilon budget must be positive")
  @Schema(description = "Epsilon budget for differential privacy", example = "1.0")
  private Double epsilonBudget;

  @Size(max = 255, message = "Category must not exceed 255 characters")
  @Schema(
      description = "Name of the category the check belongs to; created when missing",
      example = "Data Completeness")
  private String category;
}
