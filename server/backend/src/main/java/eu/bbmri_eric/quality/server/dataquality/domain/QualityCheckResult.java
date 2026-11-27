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

  @ManyToOne(fetch = FetchType.EAGER)
  @MapsId("versionId")
  @JoinColumn(name = "version_id")
  private QualityCheckVersion version;

  private Double result;

  private Double epsilon;

  private Integer sensitivity;

  /** Default constructor for JPA. */
  protected QualityCheckResult() {}

  /**
   * Creates a new quality check result.
   *
   * @param report the report this result belongs to
   * @param qualityCheck the quality check that was executed
   * @param version the version of the check that was executed
   * @param result the numeric result of the check
   */
  QualityCheckResult(
      Report report, QualityCheck qualityCheck, QualityCheckVersion version, Double result) {
    this.report = report;
    this.qualityCheck = qualityCheck;
    this.version = version;
    this.result = result;
    this.id = new QualityCheckResultId(report.getId(), qualityCheck.getId(), version.getId());
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
   * Gets the version of the quality check that was executed.
   *
   * @return the version
   */
  public QualityCheckVersion getVersion() {
    return version;
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

  /**
   * Gets the epsilon that produced the released value, as reported by the agent.
   *
   * @return the epsilon, or null if not reported
   */
  public Double getEpsilon() {
    return epsilon;
  }

  /**
   * Sets the epsilon that produced the released value.
   *
   * @param epsilon the epsilon to set
   */
  public void setEpsilon(Double epsilon) {
    this.epsilon = epsilon;
  }

  /**
   * Gets the sensitivity of the released query, as reported by the agent.
   *
   * @return the sensitivity, or null if not reported
   */
  public Integer getSensitivity() {
    return sensitivity;
  }

  /**
   * Sets the sensitivity of the released query.
   *
   * @param sensitivity the sensitivity to set
   */
  public void setSensitivity(Integer sensitivity) {
    this.sensitivity = sensitivity;
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
