package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO for updating quality check data. */
@Schema(name = "Quality Check Update", description = "Data for updating a quality check")
public class QualityCheckUpdateDTO {

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

  @Schema(description = "Threshold value for warnings", example = "0.8")
  private double warningThreshold;

  @Schema(description = "Threshold value for errors", example = "0.5")
  private double errorThreshold;

  /** Default constructor for serialization frameworks. */
  public QualityCheckUpdateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the quality check
   * @param description the description of what the check validates
   * @param warningThreshold threshold value for warnings
   * @param errorThreshold threshold value for errors
   */
  public QualityCheckUpdateDTO(
      String name, String description, double warningThreshold, double errorThreshold) {
    this.name = name;
    this.description = description;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
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
}
