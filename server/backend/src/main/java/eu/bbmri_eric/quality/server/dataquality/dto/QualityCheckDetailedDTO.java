package eu.bbmri_eric.quality.server.dataquality.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.hateoas.server.core.Relation;

/** Detailed representation of a quality check, including its versions. */
@Relation(itemRelation = "qualityCheck", collectionRelation = "qualityChecks")
public class QualityCheckDetailedDTO extends QualityCheckDTO {
  private List<QualityCheckVersionDTO> versions;

  /**
   * Constructor with all fields.
   *
   * @param name the name of the quality check
   * @param description the description of what the check validates
   * @param registeredAt when this quality check was registered
   * @param warningThreshold threshold value for warnings
   * @param errorThreshold threshold value for errors
   * @param keywords list of keywords associated with the quality check
   * @param versions list of versions of the quality check
   */
  public QualityCheckDetailedDTO(
      String name,
      String description,
      Instant registeredAt,
      double warningThreshold,
      double errorThreshold,
      List<String> keywords,
      List<QualityCheckVersionDTO> versions) {
    super(name, description, registeredAt, warningThreshold, errorThreshold, keywords);
    this.versions = versions;
  }

  public QualityCheckDetailedDTO(List<QualityCheckVersionDTO> versions) {
    this.versions = versions;
  }

  /** Default constructor for serialization frameworks. */
  public QualityCheckDetailedDTO() {}

  public List<QualityCheckVersionDTO> getVersions() {
    return versions;
  }

  public void setVersions(List<QualityCheckVersionDTO> versions) {
    this.versions = versions;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckDetailedDTO that = (QualityCheckDetailedDTO) o;
    return Objects.equals(versions, that.versions);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(versions);
  }
}
