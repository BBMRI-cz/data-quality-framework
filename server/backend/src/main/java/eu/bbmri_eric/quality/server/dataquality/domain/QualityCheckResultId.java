package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for QualityCheckResult.
 *
 * <p>This ID combines the report ID and quality check id to uniquely identify a quality check
 * result.
 */
@Embeddable
class QualityCheckResultId implements Serializable {
  private String reportId;
  private Long qualityCheckId;

  /** Default constructor for JPA. */
  protected QualityCheckResultId() {}

  /**
   * Creates a new composite ID.
   *
   * @param reportId the report ID
   * @param qualityCheckId the quality check id
   */
  QualityCheckResultId(String reportId, Long qualityCheckId) {
    this.reportId = reportId;
    this.qualityCheckId = qualityCheckId;
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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckResultId that = (QualityCheckResultId) o;
    return Objects.equals(reportId, that.reportId)
        && Objects.equals(qualityCheckId, that.qualityCheckId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reportId, qualityCheckId);
  }
}
