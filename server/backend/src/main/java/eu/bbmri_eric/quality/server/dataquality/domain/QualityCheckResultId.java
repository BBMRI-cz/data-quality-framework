package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for QualityCheckResult.
 *
 * <p>This ID combines the report ID and quality check hash to uniquely identify a quality check
 * result.
 */
@Embeddable
class QualityCheckResultId implements Serializable {
  private String reportId;
  private String qualityCheckHash;

  /** Default constructor for JPA. */
  protected QualityCheckResultId() {}

  /**
   * Creates a new composite ID.
   *
   * @param reportId the report ID
   * @param qualityCheckHash the quality check hash
   */
  QualityCheckResultId(String reportId, String qualityCheckHash) {
    this.reportId = reportId;
    this.qualityCheckHash = qualityCheckHash;
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
   * Gets the quality check hash.
   *
   * @return the quality check hash
   */
  public String getQualityCheckHash() {
    return qualityCheckHash;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckResultId that = (QualityCheckResultId) o;
    return Objects.equals(reportId, that.reportId)
        && Objects.equals(qualityCheckHash, that.qualityCheckHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reportId, qualityCheckHash);
  }
}
