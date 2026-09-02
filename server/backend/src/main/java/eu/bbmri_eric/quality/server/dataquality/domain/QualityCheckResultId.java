package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for QualityCheckResult.
 *
 * <p>This ID combines the report ID, quality check id, and version id to uniquely identify a
 * quality check result. Including the version id allows a single report to hold results for
 * multiple versions of the same quality check.
 */
@Embeddable
class QualityCheckResultId implements Serializable {
  private String reportId;
  private Long qualityCheckId;
  private Long versionId;

  /** Default constructor for JPA. */
  protected QualityCheckResultId() {}

  /**
   * Creates a new composite ID.
   *
   * @param reportId the report ID
   * @param qualityCheckId the quality check id
   * @param versionId the id of the version that was executed
   */
  QualityCheckResultId(String reportId, Long qualityCheckId, Long versionId) {
    this.reportId = reportId;
    this.qualityCheckId = qualityCheckId;
    this.versionId = versionId;
  }

  /**
   * Gets the report ID.
   *
   * @return the report ID
   */
  public String getReportId() {
    return reportId;
  }

  /**
   * Gets the quality check id.
   *
   * @return the quality check id
   */
  public Long getQualityCheckId() {
    return qualityCheckId;
  }

  /**
   * Gets the version id.
   *
   * @return the version id
   */
  public Long getVersionId() {
    return versionId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckResultId that = (QualityCheckResultId) o;
    return Objects.equals(reportId, that.reportId)
        && Objects.equals(qualityCheckId, that.qualityCheckId)
        && Objects.equals(versionId, that.versionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reportId, qualityCheckId, versionId);
  }
}
