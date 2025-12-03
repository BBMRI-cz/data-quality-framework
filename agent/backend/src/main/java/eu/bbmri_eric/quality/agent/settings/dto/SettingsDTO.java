package eu.bbmri_eric.quality.agent.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "FHIR server configuration settings")
public class SettingsDTO {

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

  @Schema(
      description = "Agent identifier",
      example = "agent-12345",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String agentId;

  public SettingsDTO() {}

  public SettingsDTO(String fhirUrl, String fhirUsername, String fhirPassword) {
    this.fhirUrl = fhirUrl;
    this.fhirUsername = fhirUsername;
    this.fhirPassword = fhirPassword;
  }
}
