package eu.bbmri_eric.quality.agent.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Portable export representation of a quality check, omitting instance-specific data (IDs, the
 * active flag, report references) so the file can be imported into another agent.
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "Quality Check Export",
    description = "A quality check entry in the portable export format")
public class QualityCheckExportDTO {

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

  @Schema(
      description = "Name of the category the check belongs to, if any",
      example = "Data Completeness")
  private String category;

  /**
   * Maps a quality check entity to its portable export representation.
   *
   * @param check the entity to map
   * @return the export DTO, with the category carried by name
   */
  public static QualityCheckExportDTO from(QualityCheck check) {
    QualityCheckExportDTO dto = new QualityCheckExportDTO();
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
