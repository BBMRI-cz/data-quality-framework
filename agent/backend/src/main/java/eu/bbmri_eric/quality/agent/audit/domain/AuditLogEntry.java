package eu.bbmri_eric.quality.agent.audit.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.bbmri_eric.quality.agent.audit.AuditAction;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AuditLogEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  private String actor;

  @Enumerated(EnumType.STRING)
  private AuditAction action;

  private String details;

  private String module;

  private Long entityId;
}
