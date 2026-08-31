package eu.bbmri_eric.quality.server.dataquality.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class QualityCheckDetailedDTO extends QualityCheckDTO {
  private List<QualityCheckVersionDTO> versions;

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
