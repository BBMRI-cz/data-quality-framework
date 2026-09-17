package eu.bbmri_eric.quality.agent.audit.dto;

import eu.bbmri_eric.quality.agent.audit.domain.AuditAction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Audit log entry Data Transfer Object")
public class AuditLogDTO {

  @Schema(
      description = "Audit log entry ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  @Schema(description = "Timestamp at which the action was performed", example = "2026-09-15T10:30:00")
  private LocalDateTime timestamp;

  @Schema(
      description = "Username of the actor who performed the action, or SYSTEM",
      example = "admin")
  private String actor;

  @Schema(description = "The type of action that was audited", example = "LOGIN_SUCCESS")
  private AuditAction action;

  @Schema(
      description = "Free-text details describing the action",
      example = "User logged in successfully from 192.168.1.10")
  private String details;

  @Schema(description = "Module in which the action occurred", example = "user")
  private String module;

  @Schema(description = "ID of the entity affected by the action, if applicable", example = "42")
  private Long entityId;
}