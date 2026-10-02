package eu.bbmri_eric.quality.agent.audit.impl;

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
   * @param auditRecord the data for the entry to record
   */
  void record(AuditRecord auditRecord);
}
