package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Records audit log entries by persisting them directly to the repository. */
@Component
class AuditRecorderImpl implements AuditRecorder {

  private static final Logger logger = LoggerFactory.getLogger(AuditRecorderImpl.class);
  private static final String SYSTEM_ACTOR = "SYSTEM";

  private final AuditLogRepository auditLogRepository;

  AuditRecorderImpl(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Override
  @Transactional
  public void record(AuditRecord auditRecord) {
    AuditLogEntry entry = new AuditLogEntry();
    entry.setTimestamp(LocalDateTime.now());
    entry.setActor(auditRecord.actor != null ? auditRecord.actor : SYSTEM_ACTOR);
    entry.setActorId(auditRecord.actorId);
    entry.setAction(auditRecord.action);
    entry.setDetails(auditRecord.details);
    entry.setModule(auditRecord.module);
    entry.setEntityId(auditRecord.entityId);

    auditLogRepository.save(entry);
    logger.debug("Recorded audit log entry: action={}, actor={}", entry.getAction(), entry.getActor());
  }
}