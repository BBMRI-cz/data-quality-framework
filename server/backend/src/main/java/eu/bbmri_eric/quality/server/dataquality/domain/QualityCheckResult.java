package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity representing the result of a quality check execution within a report.
 *
 * <p>This is a join entity that links a Report to a QualityCheck and stores the execution result.
 */
@Entity
public class QualityCheckResult {
  @EmbeddedId private QualityCheckResultId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("reportId")
  @JoinColumn(name = "report_id")
  private Report report;

  @ManyToOne(fetch = FetchType.EAGER)
  @MapsId("qualityCheckId")
  @JoinColumn(name = "quality_check_id")
  private QualityCheck qualityCheck;

  private Double result;

  /** Default constructor for JPA. */
  protected QualityCheckResult() {}

  /**
   * Creates a new quality check result.
   *
   * @param report the report this result belongs to
   * @param qualityCheck the quality check that was executed
   * @param result the numeric result of the check
   */
  QualityCheckResult(Report report, QualityCheck qualityCheck, Double result) {
    this.report = report;
    this.qualityCheck = qualityCheck;
    this.result = result;
    this.id = new QualityCheckResultId(report.getId(), qualityCheck.getId());
  }

  /**
   * Gets the composite ID of this result.
   *
   * @return the result ID
   */
  QualityCheckResultId getId() {
    return id;
  }

  /**
   * Gets the report this result belongs to.
   *
   * @return the report
   */
  Report getReport() {
    return report;
  }

  /**
   * Gets the quality check that was executed.
   *
   * @return the quality check
   */
  public QualityCheck getQualityCheck() {
    return qualityCheck;
  }

  /**
   * Gets the numeric result of the quality check.
   *
   * @return the result value
   */
  public Double getResult() {
    return result;
  }

  /**
   * Sets the numeric result of the quality check.
   *
   * @param result the result value to set
   */
  public void setResult(Double result) {
    this.result = result;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckResult that = (QualityCheckResult) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
