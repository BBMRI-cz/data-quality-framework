package eu.bbmri_eric.quality.server.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Data-independent description of the differential-privacy configuration that produced a report:
 * the configured mechanism and parameters plus the budget split, as reported by the agent. All
 * fields are optional; agents that do not report them leave the whole object absent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Privacy parameters that produced a report")
public class PrivacyMetadataDTO {

  @Schema(
      description = "Noise mechanism used for the check results",
      example = "LAPLACE",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String mechanism;

  @Schema(
      description = "Configured total privacy budget (epsilon) of the report",
      example = "1.0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private Double epsilon;

  @Schema(
      description = "Configured delta parameter of the privacy budget",
      example = "1.0E-8",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private Double delta;

  @Schema(
      description = "Configured minimum threshold for low count suppression",
      example = "10",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private Integer minThreshold;

  @Schema(
      description = "Share of the total epsilon split among the check results",
      example = "0.8",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private Double checkResultsEpsilon;

  @Schema(
      description = "Epsilon spent per released entity total",
      example = "0.1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private Double entityTotalEpsilon;

  @Schema(
      description = "Noise mechanism always used for the entity totals",
      example = "LAPLACE",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String entityTotalMechanism;

  public PrivacyMetadataDTO() {}

  public String getMechanism() {
    return mechanism;
  }

  public void setMechanism(String mechanism) {
    this.mechanism = mechanism;
  }

  public Double getEpsilon() {
    return epsilon;
  }

  public void setEpsilon(Double epsilon) {
    this.epsilon = epsilon;
  }

  public Double getDelta() {
    return delta;
  }

  public void setDelta(Double delta) {
    this.delta = delta;
  }

  public Integer getMinThreshold() {
    return minThreshold;
  }

  public void setMinThreshold(Integer minThreshold) {
    this.minThreshold = minThreshold;
  }

  public Double getCheckResultsEpsilon() {
    return checkResultsEpsilon;
  }

  public void setCheckResultsEpsilon(Double checkResultsEpsilon) {
    this.checkResultsEpsilon = checkResultsEpsilon;
  }

  public Double getEntityTotalEpsilon() {
    return entityTotalEpsilon;
  }

  public void setEntityTotalEpsilon(Double entityTotalEpsilon) {
    this.entityTotalEpsilon = entityTotalEpsilon;
  }

  public String getEntityTotalMechanism() {
    return entityTotalMechanism;
  }

  public void setEntityTotalMechanism(String entityTotalMechanism) {
    this.entityTotalMechanism = entityTotalMechanism;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (PrivacyMetadataDTO) obj;
    return Objects.equals(this.mechanism, that.mechanism)
        && Objects.equals(this.epsilon, that.epsilon)
        && Objects.equals(this.delta, that.delta)
        && Objects.equals(this.minThreshold, that.minThreshold)
        && Objects.equals(this.checkResultsEpsilon, that.checkResultsEpsilon)
        && Objects.equals(this.entityTotalEpsilon, that.entityTotalEpsilon)
        && Objects.equals(this.entityTotalMechanism, that.entityTotalMechanism);
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

  @Override
  public String toString() {
    return "PrivacyMetadataDTO["
        + "mechanism="
        + mechanism
        + ", "
        + "epsilon="
        + epsilon
        + ", "
        + "delta="
        + delta
        + ", "
        + "minThreshold="
        + minThreshold
        + ", "
        + "checkResultsEpsilon="
        + checkResultsEpsilon
        + ", "
        + "entityTotalEpsilon="
        + entityTotalEpsilon
        + ", "
        + "entityTotalMechanism="
        + entityTotalMechanism
        + ']';
  }
}
