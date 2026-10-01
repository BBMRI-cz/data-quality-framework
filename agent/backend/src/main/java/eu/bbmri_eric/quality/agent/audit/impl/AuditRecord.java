package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * Holds the data for a single audit log entry to be recorded via {@link AuditRecorder}. The getters
 * let {@code ModelMapper} map it onto an {@code AuditLogEntry}.
 */
@Getter
class AuditRecord {

  final AuditAction action;
  final String actor;
  final Long actorId;
  final String details;
  final String module;
  final Long entityId;
  final LocalDateTime timestamp;

  private AuditRecord(Builder builder) {
    this.action = builder.action;
    this.actor = builder.actor;
    this.actorId = builder.actorId;
    this.details = builder.details;
    this.module = builder.module;
    this.entityId = builder.entityId;
    this.timestamp = builder.timestamp;
  }

  static Builder of(AuditAction action) {
    return new Builder(action);
  }

  static class Builder {

    private final AuditAction action;
    private String actor;
    private Long actorId;
    private String details;
    private String module;
    private Long entityId;
    private LocalDateTime timestamp;

    private Builder(AuditAction action) {
      this.action = action;
    }

    Builder actor(String username, Long id) {
      this.actor = username;
      this.actorId = id;
      return this;
    }

    Builder details(String details) {
      this.details = details;
      return this;
    }

    Builder module(String module) {
      this.module = module;
      return this;
    }

    Builder entityId(Long entityId) {
      this.entityId = entityId;
      return this;
    }

    Builder timestamp(LocalDateTime timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    AuditRecord build() {
      return new AuditRecord(this);
    }
  }
}
