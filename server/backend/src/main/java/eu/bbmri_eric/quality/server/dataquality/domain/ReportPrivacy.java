package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Data-independent privacy configuration that produced a report, as reported by the agent. All
 * fields are optional: agents predating report privacy metadata store an all-null (absent)
 * embeddable.
 */
@Embeddable
public class ReportPrivacy {

  private String mechanism;

  private Double epsilon;

  private Double delta;

  private Integer minThreshold;

  private Double checkResultsEpsilon;

  private Double entityTotalEpsilon;

  private String entityTotalMechanism;

  /** Default constructor for JPA. */
  public ReportPrivacy() {}

  /**
   * Gets the noise mechanism used for the check results.
   *
   * @return the mechanism name, or null if not reported
   */
  public String getMechanism() {
    return mechanism;
  }

  /**
   * Sets the noise mechanism used for the check results.
   *
   * @param mechanism the mechanism name
   */
  public void setMechanism(String mechanism) {
    this.mechanism = mechanism;
  }

  /**
   * Gets the configured total privacy budget (epsilon) of the report.
   *
   * @return the epsilon, or null if not reported
   */
  public Double getEpsilon() {
    return epsilon;
  }

  /**
   * Sets the configured total privacy budget (epsilon) of the report.
   *
   * @param epsilon the epsilon
   */
  public void setEpsilon(Double epsilon) {
    this.epsilon = epsilon;
  }

  /**
   * Gets the configured delta parameter of the privacy budget.
   *
   * @return the delta, or null if not reported
   */
  public Double getDelta() {
    return delta;
  }

  /**
   * Sets the configured delta parameter of the privacy budget.
   *
   * @param delta the delta
   */
  public void setDelta(Double delta) {
    this.delta = delta;
  }

  /**
   * Gets the configured minimum threshold for low count suppression.
   *
   * @return the threshold, or null if not reported
   */
  public Integer getMinThreshold() {
    return minThreshold;
  }

  /**
   * Sets the configured minimum threshold for low count suppression.
   *
   * @param minThreshold the threshold
   */
  public void setMinThreshold(Integer minThreshold) {
    this.minThreshold = minThreshold;
  }

  /**
   * Gets the share of the total epsilon split among the check results.
   *
   * @return the check results epsilon share, or null if not reported
   */
  public Double getCheckResultsEpsilon() {
    return checkResultsEpsilon;
  }

  /**
   * Sets the share of the total epsilon split among the check results.
   *
   * @param checkResultsEpsilon the check results epsilon share
   */
  public void setCheckResultsEpsilon(Double checkResultsEpsilon) {
    this.checkResultsEpsilon = checkResultsEpsilon;
  }

  /**
   * Gets the epsilon spent per released entity total.
   *
   * @return the entity total epsilon, or null if not reported
   */
  public Double getEntityTotalEpsilon() {
    return entityTotalEpsilon;
  }

  /**
   * Sets the epsilon spent per released entity total.
   *
   * @param entityTotalEpsilon the entity total epsilon
   */
  public void setEntityTotalEpsilon(Double entityTotalEpsilon) {
    this.entityTotalEpsilon = entityTotalEpsilon;
  }

  /**
   * Gets the noise mechanism the entity totals always use.
   *
   * @return the mechanism name, or null if not reported
   */
  public String getEntityTotalMechanism() {
    return entityTotalMechanism;
  }

  /**
   * Sets the noise mechanism the entity totals always use.
   *
   * @param entityTotalMechanism the mechanism name
   */
  public void setEntityTotalMechanism(String entityTotalMechanism) {
    this.entityTotalMechanism = entityTotalMechanism;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ReportPrivacy that = (ReportPrivacy) o;
    return Objects.equals(mechanism, that.mechanism)
        && Objects.equals(epsilon, that.epsilon)
        && Objects.equals(delta, that.delta)
        && Objects.equals(minThreshold, that.minThreshold)
        && Objects.equals(checkResultsEpsilon, that.checkResultsEpsilon)
        && Objects.equals(entityTotalEpsilon, that.entityTotalEpsilon)
        && Objects.equals(entityTotalMechanism, that.entityTotalMechanism);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        mechanism,
        epsilon,
        delta,
        minThreshold,
        checkResultsEpsilon,
        entityTotalEpsilon,
        entityTotalMechanism);
  }
}
