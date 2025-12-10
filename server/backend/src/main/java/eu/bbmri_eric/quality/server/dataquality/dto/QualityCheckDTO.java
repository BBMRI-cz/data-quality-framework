package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.springframework.hateoas.server.core.Relation;

/** DTO for quality check data. */
@Schema(name = "Quality Check", description = "A quality check definition")
@Relation(itemRelation = "qualityCheck", collectionRelation = "qualityChecks")
public class QualityCheckDTO {

  @Schema(
      description = "Hash identifying the quality check",
      example = "abc123def456",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String hash;

  @Schema(
      description = "Name of the quality check",
      example = "Patient Count Validation",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(
      description = "Description of what the check validates",
      example = "Validates that patient count is within expected range")
  private String description;

  @Schema(description = "When this quality check was registered", example = "2023-10-13T10:30:00")
  private LocalDateTime registeredAt;

  @Schema(description = "Threshold value for warnings", example = "0.8")
  private double warningThreshold;

  @Schema(description = "Threshold value for errors", example = "0.5")
  private double errorThreshold;

  /** Default constructor for serialization frameworks. */
  public QualityCheckDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param hash the unique hash identifying the quality check
   * @param name the name of the quality check
   * @param description the description of what the check validates
   * @param registeredAt when this quality check was registered
   * @param warningThreshold threshold value for warnings
   * @param errorThreshold threshold value for errors
   */
  public QualityCheckDTO(
      String hash,
      String name,
      String description,
      LocalDateTime registeredAt,
      double warningThreshold,
      double errorThreshold) {
    this.hash = hash;
    this.name = name;
    this.description = description;
    this.registeredAt = registeredAt;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
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

  public LocalDateTime getRegisteredAt() {
    return registeredAt;
  }

  public void setRegisteredAt(LocalDateTime registeredAt) {
    this.registeredAt = registeredAt;
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
