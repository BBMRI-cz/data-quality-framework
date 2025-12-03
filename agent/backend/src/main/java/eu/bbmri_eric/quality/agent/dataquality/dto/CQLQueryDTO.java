package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** DTO for CQL Query entity. */
@Schema(name = "CQL Query", description = "A CQL-based data quality check")
public class CQLQueryDTO {

  @Schema(description = "Unique identifier of the CQL query", example = "1")
  private Long id;

  @Schema(description = "Name of the CQL query", example = "Patient Age Validation")
  private String name;

  @Schema(
      description = "Description of what the check validates",
      example = "Validates patient ages are within acceptable range")
  private String description;

  @Schema(
      description = "CQL query string",
      example = "library PatientAgeValidation version '1.0.0'...")
  private String query;

  @Schema(description = "Warning threshold percentage", example = "10")
  private int warningThreshold;

  @Schema(description = "Error threshold percentage", example = "30")
  private int errorThreshold;

  @Schema(description = "Epsilon budget for differential privacy", example = "1.0")
  private float epsilonBudget;

  public CQLQueryDTO() {}

  public CQLQueryDTO(
      Long id,
      String name,
      String description,
      String query,
      int warningThreshold,
      int errorThreshold,
      float epsilonBudget) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.query = query;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.epsilonBudget = epsilonBudget;
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

  public int getWarningThreshold() {
    return warningThreshold;
  }

  public void setWarningThreshold(int warningThreshold) {
    this.warningThreshold = warningThreshold;
  }

  public int getErrorThreshold() {
    return errorThreshold;
  }

  public void setErrorThreshold(int errorThreshold) {
    this.errorThreshold = errorThreshold;
  }

  public float getEpsilonBudget() {
    return epsilonBudget;
  }

  public void setEpsilonBudget(float epsilonBudget) {
    this.epsilonBudget = epsilonBudget;
  }
}
