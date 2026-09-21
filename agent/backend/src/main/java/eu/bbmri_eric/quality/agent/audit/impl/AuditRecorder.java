package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;

/**
 * Records audit log entries.
 *
 * <p>Package-private by design: recording is meant to happen through {@code AuditAspect} (via
 * {@code @Audited}) or an existing domain event listener, not by other modules calling this
 * directly. Use {@link eu.bbmri_eric.quality.agent.audit.AuditService} to query entries instead.
 */
interface AuditRecorder {

  /**
   * Records an audit log entry.
   *
   * @param action the type of action performed
   * @param actor the username of the actor who performed the action, or {@code null} if the action
   *     was performed by the system
   * @param details free-text details describing the action
   */
  void record(AuditAction action, String actor, String details);

  /**
   * Records an audit log entry with module and entity context.
   *
   * @param action the type of action performed
   * @param actor the username of the actor who performed the action, or {@code null} if the action
   *     was performed by the system
   * @param details free-text details describing the action
   * @param module the module in which the action occurred, or {@code null}
   * @param entityId the ID of the entity affected by the action, or {@code null}
   */
  void record(AuditAction action, String actor, String details, String module, Long entityId);

  /**
   * Records an audit log entry with module, entity, and actor identity context.
   *
   * @param action the type of action performed
   * @param actor the username of the actor who performed the action, or {@code null} if the action
   *     was performed by the system
   * @param actorId the ID of the {@code User} who performed the action, or {@code null} if unknown
   *     or the action was performed by the system
   * @param details free-text details describing the action
   * @param module the module in which the action occurred, or {@code null}
   * @param entityId the ID of the entity affected by the action, or {@code null}
   */
  void record(
      AuditAction action, String actor, Long actorId, String details, String module, Long entityId);
}
