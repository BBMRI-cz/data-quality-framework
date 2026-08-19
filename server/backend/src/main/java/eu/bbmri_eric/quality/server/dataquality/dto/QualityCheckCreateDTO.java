package eu.bbmri_eric.quality.server.dataquality.dto;

import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO for creating a new quality check. */
@Schema(name = "Quality Check Create", description = "Data for creating a new quality check")
public class QualityCheckCreateDTO {

  @NotBlank(message = "Name cannot be blank")
  @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
  @Schema(
      description = "Name of the quality check",
      example = "Patient Count Validation",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  @Schema(
      description = "Description of what the check validates",
      example = "Validates that patient count is within expected range")
  private String description;

  @NotBlank(message = "Query cannot be blank")
  @Schema(
      description = "Query associated with the quality check",
      example = "SELECT count(*) ...",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String query;

  @NotNull(message = "Type cannot be null")
  @Schema(
      description = "Type of query used by the quality check",
      example = "SQL",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private QualityCheckType type;

  @Schema(description = "Threshold value for warnings", example = "0.8")
  private double warningThreshold;

  @Schema(description = "Threshold value for errors", example = "0.5")
  private double errorThreshold;

  @Schema(description = "Category ID for grouping quality checks", example = "1")
  private Long categoryId;

  /** Default constructor for serialization frameworks. */
  public QualityCheckCreateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the quality check
   * @param description the description of what the check validates
   * @param query the query associated with the check, or null
   * @param type the type of query used by the check, or null
   * @param warningThreshold threshold value for warnings
   * @param errorThreshold threshold value for errors
   * @param categoryId the category ID for grouping quality checks
   */
  public QualityCheckCreateDTO(
      String name,
      String description,
      String query,
      QualityCheckType type,
      double warningThreshold,
      double errorThreshold,
      Long categoryId) {
    this.name = name;
    this.description = description;
    this.query = query;
    this.type = type;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.categoryId = categoryId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public QualityCheckType getType() {
    return type;
  }

  public void setType(QualityCheckType type) {
    this.type = type;
  }

  public double getWarningThreshold() {
    return warningThreshold;
  }

  public void setWarningThreshold(double warningThreshold) {
    this.warningThreshold = warningThreshold;
  }

  public double getErrorThreshold() {
    return errorThreshold;
  }

  public void setErrorThreshold(double errorThreshold) {
    this.errorThreshold = errorThreshold;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }
}
