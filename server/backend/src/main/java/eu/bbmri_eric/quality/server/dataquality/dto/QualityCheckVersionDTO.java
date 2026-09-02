package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

/** DTO for quality check version data. */
@Schema(
    name = "Quality Check Version",
    description = "An immutable version of a quality check query")
@Relation(itemRelation = "qualityCheckVersion", collectionRelation = "qualityCheckVersions")
public class QualityCheckVersionDTO {

  @Schema(
      description = "Numeric id of the quality check version",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(
      description = "Version number of the quality check",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private int version;

  @Schema(
      description = "The query text of this version",
      example = "SELECT COUNT(*) FROM patients WHERE gender = 'F'",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String query;

  @Schema(
      description = "SHA-256 hash of the query",
      example = "a1b2c3d4e5f6...",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String hash;

  /** Default constructor for serialization frameworks. */
  public QualityCheckVersionDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param id the numeric id of the version
   * @param version the version number
   * @param query the query text
   * @param hash the hash of the query
   */
  public QualityCheckVersionDTO(Long id, int version, String query, String hash) {
    this.id = id;
    this.version = version;
    this.query = query;
    this.hash = hash;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }
}
