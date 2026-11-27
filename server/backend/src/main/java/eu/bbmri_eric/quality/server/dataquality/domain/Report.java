package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a data quality report.
 *
 * <p>Each report is automatically assigned a unique identifier and timestamp upon creation. Reports
 * are associated with an agent that generated them.
 */
@Entity
public class Report {
  @Id private final String id = UUID.randomUUID().toString();

  private final Instant timestamp = Instant.now();

  @Column(name = "agent_id", insertable = false, updatable = false)
  private String agentId;

  @Column(name = "total_patients")
  private Integer totalPatients;

  @Column(name = "total_samples")
  private Integer totalSamples;

  @Column(name = "agent_version")
  private String agentVersion;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "mechanism", column = @Column(name = "privacy_mechanism")),
    @AttributeOverride(name = "epsilon", column = @Column(name = "privacy_epsilon")),
    @AttributeOverride(name = "delta", column = @Column(name = "privacy_delta")),
    @AttributeOverride(name = "minThreshold", column = @Column(name = "privacy_min_threshold")),
    @AttributeOverride(
        name = "checkResultsEpsilon",
        column = @Column(name = "privacy_check_results_epsilon")),
    @AttributeOverride(
        name = "entityTotalEpsilon",
        column = @Column(name = "privacy_entity_total_epsilon")),
    @AttributeOverride(
        name = "entityTotalMechanism",
        column = @Column(name = "privacy_entity_total_mechanism"))
  })
  private ReportPrivacy privacy;

  @Column(name = "total_patients_epsilon")
  private Double totalPatientsEpsilon;

  @Column(name = "total_patients_sensitivity")
  private Integer totalPatientsSensitivity;

  @Column(name = "total_samples_epsilon")
  private Double totalSamplesEpsilon;

  @Column(name = "total_samples_sensitivity")
  private Integer totalSamplesSensitivity;

  @OneToMany(
      mappedBy = "report",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<QualityCheckResult> qualityCheckResults = new ArrayList<>();

  /** Creates a new report. */
  public Report() {}

  /**
   * Gets the unique identifier of this report.
   *
   * @return the report ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the timestamp when this report was created.
   *
   * @return the creation timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the identifier of the agent that generated this report.
   *
   * @return the agent ID
   */
  public String getAgentId() {
    return agentId;
  }

  /**
   * Sets the identifier of the agent that generated this report.
   *
   * @param agentId the agent ID to set
   * @throws IllegalArgumentException if agentId is null or empty
   */
  public void setAgentId(String agentId) {
    if (agentId == null || agentId.trim().isEmpty()) {
      throw new IllegalArgumentException("Agent ID cannot be null or empty");
    }
    this.agentId = agentId;
  }

  /**
   * Gets the total number of patients.
   *
   * @return the total number of patients
   */
  public Integer getTotalPatients() {
    return totalPatients;
  }

  /**
   * Sets the total number of patients.
   *
   * @param totalPatients the total number of patients
   */
  public void setTotalPatients(Integer totalPatients) {
    this.totalPatients = totalPatients;
  }

  /**
   * Gets the total number of samples.
   *
   * @return the total number of samples
   */
  public Integer getTotalSamples() {
    return totalSamples;
  }

  /**
   * Sets the total number of samples.
   *
   * @param totalSamples the total number of samples
   */
  public void setTotalSamples(Integer totalSamples) {
    this.totalSamples = totalSamples;
  }

  /**
   * Gets the version of the agent that produced this report.
   *
   * @return the agent version, or null if not reported
   */
  public String getAgentVersion() {
    return agentVersion;
  }

  /**
   * Sets the version of the agent that produced this report.
   *
   * @param agentVersion the agent version to set
   */
  public void setAgentVersion(String agentVersion) {
    this.agentVersion = agentVersion;
  }

  /**
   * Gets the privacy parameters that produced this report.
   *
   * @return the privacy metadata, or null if not reported
   */
  public ReportPrivacy getPrivacy() {
    return privacy;
  }

  /**
   * Sets the privacy parameters that produced this report.
   *
   * @param privacy the privacy metadata to set
   */
  public void setPrivacy(ReportPrivacy privacy) {
    this.privacy = privacy;
  }

  /**
   * Gets the epsilon spent on the released patient total.
   *
   * @return the epsilon, or null if not reported
   */
  public Double getTotalPatientsEpsilon() {
    return totalPatientsEpsilon;
  }

  /**
   * Sets the epsilon spent on the released patient total.
   *
   * @param totalPatientsEpsilon the epsilon to set
   */
  public void setTotalPatientsEpsilon(Double totalPatientsEpsilon) {
    this.totalPatientsEpsilon = totalPatientsEpsilon;
  }

  /**
   * Gets the sensitivity of the patient total query.
   *
   * @return the sensitivity, or null if not reported
   */
  public Integer getTotalPatientsSensitivity() {
    return totalPatientsSensitivity;
  }

  /**
   * Sets the sensitivity of the patient total query.
   *
   * @param totalPatientsSensitivity the sensitivity to set
   */
  public void setTotalPatientsSensitivity(Integer totalPatientsSensitivity) {
    this.totalPatientsSensitivity = totalPatientsSensitivity;
  }

  /**
   * Gets the epsilon spent on the released sample total.
   *
   * @return the epsilon, or null if not reported
   */
  public Double getTotalSamplesEpsilon() {
    return totalSamplesEpsilon;
  }

  /**
   * Sets the epsilon spent on the released sample total.
   *
   * @param totalSamplesEpsilon the epsilon to set
   */
  public void setTotalSamplesEpsilon(Double totalSamplesEpsilon) {
    this.totalSamplesEpsilon = totalSamplesEpsilon;
  }

  /**
   * Gets the sensitivity of the sample total query.
   *
   * @return the sensitivity, or null if not reported
   */
  public Integer getTotalSamplesSensitivity() {
    return totalSamplesSensitivity;
  }

  /**
   * Sets the sensitivity of the sample total query.
   *
   * @param totalSamplesSensitivity the sensitivity to set
   */
  public void setTotalSamplesSensitivity(Integer totalSamplesSensitivity) {
    this.totalSamplesSensitivity = totalSamplesSensitivity;
  }

  /**
   * Gets an unmodifiable list of quality check results.
   *
   * @return the quality check results
   */
  public List<QualityCheckResult> getQualityCheckResults() {
    return Collections.unmodifiableList(qualityCheckResults);
  }

  /**
   * Adds a quality check result to this report.
   *
   * @param version the version of the quality check that was executed
   * @param result the numeric result of the check
   * @return the created quality check result
   */
  public QualityCheckResult addQualityCheckResult(QualityCheckVersion version, Double result) {
    QualityCheckResult checkResult =
        new QualityCheckResult(this, version.getQualityCheck(), version, result);
    qualityCheckResults.add(checkResult);
    return checkResult;
  }

  /**
   * Removes a quality check result from this report.
   *
   * @param result the result to remove
   */
  public void removeQualityCheckResult(QualityCheckResult result) {
    qualityCheckResults.remove(result);
  }

  /** Clears all quality check results from this report. */
  public void clearQualityCheckResults() {
    qualityCheckResults.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Report report = (Report) o;
    return Objects.equals(id, report.id)
        && Objects.equals(timestamp, report.timestamp)
        && Objects.equals(agentId, report.agentId)
        && Objects.equals(totalPatients, report.totalPatients)
        && Objects.equals(totalSamples, report.totalSamples);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, timestamp, agentId, totalPatients, totalSamples);
  }
}
