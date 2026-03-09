package eu.bbmri_eric.quality.agent.settings.dto;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DiffPrivacySettingsDTO {
  @NotNull(message = "Epsilon is required")
  @Positive(message = "Epsilon must be positive")
  @Schema(
      description = "Privacy budget (ε)",
      example = "3.0",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Double epsilon;

  @NotNull(message = "Delta is required")
  @Positive(message = "Delta must be positive")
  @Schema(
      description = "Delta parameter (δ) - probability of privacy failure",
      example = "1e-8",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Double delta;

  @NotNull(message = "Minimum threshold is required")
  @Min(value = 0, message = "Minimum threshold must be non-negative")
  @Schema(
      description = "Minimum threshold for low count suppression",
      example = "50",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer minThreshold;

  @NotNull(message = "Noise mechanism is required")
  @Schema(
      description = "Noise mechanism (LAPLACE or GAUSSIAN)",
      example = "LAPLACE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private NoiseMechanism noiseMechanism;

  public DiffPrivacySettingsDTO() {}
}
