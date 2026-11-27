package eu.bbmri_eric.quality.agent.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Data-independent description of the differential-privacy configuration that produced a report:
 * the configured mechanism and parameters plus the budget split, so a recipient can interpret the
 * noise scale of the released values. Nullable fields are omitted from the payload.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Privacy parameters that produced an obfuscated report")
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
}
