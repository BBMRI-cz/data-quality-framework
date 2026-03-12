package eu.bbmri_eric.quality.agent.settings.dto;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Application settings Data Transfer Object")
public class SettingsDTO {

  @Schema(
      description = "Agent identifier",
      example = "agent-12345",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String agentId;

  @NotBlank(message = "FHIR URL is required")
  @Size(max = 500, message = "FHIR URL must not exceed 500 characters")
  @Pattern(regexp = "^https?://.*", message = "FHIR URL must be a valid HTTP or HTTPS URL")
  @Schema(
      description = "FHIR server URL",
      example = "http://localhost:8080/fhir",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String fhirUrl;

  @NotBlank(message = "FHIR username is required")
  @Size(max = 100, message = "FHIR username must not exceed 100 characters")
  @Schema(
      description = "FHIR server username",
      example = "admin",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String fhirUsername;

  @NotBlank(message = "FHIR password is required")
  @Size(max = 100, message = "FHIR password must not exceed 100 characters")
  @Schema(
      description = "FHIR server password (Base64-encoded)",
      example = "cGFzc3dvcmQ=",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String fhirPassword;

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

  public SettingsDTO() {}
}
