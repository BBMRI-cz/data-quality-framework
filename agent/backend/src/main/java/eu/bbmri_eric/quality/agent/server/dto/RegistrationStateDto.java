package eu.bbmri_eric.quality.agent.server.dto;

import eu.bbmri_eric.quality.agent.server.domain.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for the registration lifecycle state of this agent with a central server.
 *
 * <p>Complements {@link ServerDto}, whose {@code status} field describes connection and approval
 * state only.
 */
@Setter
@Getter
@Schema(description = "Registration lifecycle state of the agent with a central server")
public class RegistrationStateDto {

  @Schema(
      description =
          "Registration lifecycle state; ACTIVE is derived from a successful registration "
              + "combined with an approved server status and never persisted",
      example = "REGISTERED_PENDING_APPROVAL")
  private RegistrationStatus status;

  @Schema(
      description = "Classified reason of the last failed registration, if any",
      example = "UNREACHABLE")
  private String lastErrorReason;

  @Schema(
      description = "Full failure description of the last failed registration, if any",
      example = "Registration failed: The central server could not be reached")
  private String lastErrorMessage;

  @Schema(description = "When the central server accepted the registration, if ever")
  private LocalDateTime registeredAt;

  @Schema(description = "When the next automatic registration retry is scheduled, if any")
  private LocalDateTime nextRetryAt;

  @Schema(
      description =
          "Number of registration attempts made (in-process attempts of the current run are not "
              + "counted; automatic sweep rounds are), zero when no attempt was recorded",
      example = "2")
  private Integer attempts;

  /** Default constructor. */
  public RegistrationStateDto() {}
}
