package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** DTO for creating a new quality check version. */
@Schema(
    name = "Quality Check Version Create",
    description = "Data for creating a quality check version")
public class QualityCheckVersionCreateDTO {

  @NotBlank(message = "Query cannot be blank")
  @Schema(
      description = "Query text of the version",
      example = "SELECT COUNT(*) FROM patients WHERE gender = 'F'",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String query;

  @Min(value = 1, message = "Version must be at least 1")
  @Schema(
      description = "Version number; if omitted, the next available version is assigned",
      example = "1")
  private Integer version;

  /** Default constructor for serialization frameworks. */
  public QualityCheckVersionCreateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param query the query text
   * @param version the version number, or null to auto-assign
   */
  public QualityCheckVersionCreateDTO(String query, Integer version) {
    this.query = query;
    this.version = version;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
