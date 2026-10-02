package eu.bbmri_eric.quality.agent.audit.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.bbmri_eric.quality.agent.audit.AuditAction;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single audit log entry, recording an action performed by a user or system.
 *
 * <p>This entity captures the details of an action, including the actor, timestamp, action type,
 * and any relevant details or associated entity IDs. It is used for auditing and tracking changes
 * within the system.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  private String actor;

  /** The ID of the {@code User} who performed the action, or {@code null} if unknown/system. */
  private Long actorId;

  @Enumerated(EnumType.STRING)
  private AuditAction action;

  private String details;

  private String module;

  private Long entityId;
}
