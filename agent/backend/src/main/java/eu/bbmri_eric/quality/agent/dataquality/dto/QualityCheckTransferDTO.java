package eu.bbmri_eric.quality.agent.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Portable representation of a quality check, shared by the export and the import file. It omits
 * instance-specific data (IDs, the active flag, report references) and carries the category by
 * name, so a file exported from one agent can be imported into another.
 */
@Setter
@Getter
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "Quality Check Transfer",
    description = "A quality check entry in the portable export/import file format")
public class QualityCheckTransferDTO {

  @NotBlank(message = "Name is required")
  @Size(max = 255, message = "Name must not exceed 255 characters")
  @Schema(description = "Name of the quality check", example = "Patient Age Validation")
  private String name;

  @NotBlank(message = "Description is required")
  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  @Schema(
      description = "Description of what the check validates",
      example = "Validates patient ages are within acceptable range")
  private String description;

  @Schema(
      description =
          "Query string; required for every type except JAVA, whose query is implemented in the"
              + " agent",
      example = "library PatientAgeValidation version '1.0.0'...")
  private String query;

  @Schema(description = "Type of quality check, CQL when absent", example = "CQL")
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
      description = "Name of the category the check belongs to, if any",
      example = "Data Completeness")
  private String category;

  /**
   * Maps a quality check to its portable representation.
   *
   * @param check the check to map
   * @return the transfer DTO, with the category carried by name
   */
  public static QualityCheckTransferDTO from(QualityCheckDTO check) {
    QualityCheckTransferDTO dto = new QualityCheckTransferDTO();
    dto.setName(check.getName());
    dto.setDescription(check.getDescription());
    dto.setQuery(check.getQuery());
    dto.setType(check.getType());
    dto.setWarningThreshold(check.getWarningThreshold());
    dto.setErrorThreshold(check.getErrorThreshold());
    dto.setEpsilonBudget(check.getEpsilonBudget());
    dto.setCategory(check.getCategory() == null ? null : check.getCategory().getName());
    return dto;
  }
}
