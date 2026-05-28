package eu.bbmri_eric.quality.agent.settings.dto;

import eu.bbmri_eric.quality.agent.settings.DatabaseType;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
@Schema(description = "Application settings Data Transfer Object")
public class SettingsDTO {

  @Schema(
      description = "Agent identifier",
      example = "agent-12345",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String agentId;

  @Size(max = 500, message = "FHIR URL must not exceed 500 characters")
  @Pattern(regexp = "^$|^https?://.*", message = "FHIR URL must be a valid HTTP or HTTPS URL")
  @Schema(
      description = "FHIR server URL",
      example = "http://localhost:8080/fhir",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String fhirUrl;

  @Size(max = 100, message = "FHIR username must not exceed 100 characters")
  @Schema(
      description = "FHIR server username",
      example = "admin",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String fhirUsername;

  @Size(max = 100, message = "FHIR password must not exceed 100 characters")
  @Schema(
      description = "FHIR server password (Base64-encoded)",
      example = "cGFzc3dvcmQ=",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String fhirPassword;

  @Positive(message = "Epsilon must be positive")
  @Schema(description = "Privacy budget (ε)", example = "3.0")
  private Double epsilon;

  @Positive(message = "Delta must be positive")
  @Schema(description = "Delta parameter (δ) - probability of privacy failure", example = "1e-8")
  private Double delta;

  @Min(value = 0, message = "Minimum threshold must be non-negative")
  @Schema(
      description = "Minimum threshold for low count suppression",
      example = "50",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer minThreshold;

  @Schema(description = "Noise mechanism (LAPLACE or GAUSSIAN)", example = "LAPLACE")
  private NoiseMechanism noiseMechanism;

  @Schema(description = "Database type (FHIR or SQL)", example = "FHIR")
  private DatabaseType databaseType;

  @Size(max = 500, message = "SQL URL must not exceed 500 characters")
  @Pattern(regexp = "^$|^jdbc:.*", message = "SQL URL must be a valid JDBC URL")
  @Schema(
      description = "SQL database JDBC URL",
      example = "jdbc:postgresql://localhost:5432/quality",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String sqlUrl;

  @Size(max = 100, message = "SQL username must not exceed 100 characters")
  @Schema(
      description = "SQL database username",
      example = "dbuser",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String sqlUsername;

  @Size(max = 100, message = "SQL password must not exceed 100 characters")
  @Schema(
      description = "SQL database password (Base64-encoded)",
      example = "cGFzc3dvcmQ=",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String sqlPassword;
}
