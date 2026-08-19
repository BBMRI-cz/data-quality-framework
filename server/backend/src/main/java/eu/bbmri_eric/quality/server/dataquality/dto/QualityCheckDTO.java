package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
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

  @Schema(description = "Query associated with the quality check", example = "SELECT count(*) ...")
  private String query;

  @Schema(description = "When this quality check was registered", example = "2023-10-13T10:30:00Z")
  private Instant registeredAt;

  @Schema(description = "Threshold value for warnings", example = "0.8")
  private double warningThreshold;

  @Schema(description = "Threshold value for errors", example = "0.5")
  private double errorThreshold;

  @Schema(description = "Category for grouping quality checks")
  private CategoryDTO category;

  @Schema(description = "List of keywords associated with this quality check")
  private List<String> keywords;

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
   * @param keywords list of keywords associated with the quality check
   */
  public QualityCheckDTO(
      String hash,
      String name,
      String description,
      Instant registeredAt,
      double warningThreshold,
      double errorThreshold,
      List<String> keywords) {
    this.hash = hash;
    this.name = name;
    this.description = description;
    this.registeredAt = registeredAt;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.keywords = keywords;
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

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public Instant getRegisteredAt() {
    return registeredAt;
  }

  public void setRegisteredAt(Instant registeredAt) {
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

  public CategoryDTO getCategory() {
    return category;
  }

  public void setCategory(CategoryDTO category) {
    this.category = category;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }
}
