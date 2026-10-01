package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import java.time.LocalDateTime;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
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
  private final ModelMapper modelMapper;

  AuditRecorderImpl(AuditLogRepository auditLogRepository, ModelMapper modelMapper) {
    this.auditLogRepository = auditLogRepository;
    this.modelMapper = modelMapper;
    modelMapper.createTypeMap(
        AuditRecord.class,
        AuditLogEntry.class,
        modelMapper.getConfiguration().copy().setMatchingStrategy(MatchingStrategies.STRICT));
  }

  @Override
  @Transactional
  public void record(AuditRecord auditRecord) {
    AuditLogEntry entry = modelMapper.map(auditRecord, AuditLogEntry.class);
    if (entry.getTimestamp() == null) {
      entry.setTimestamp(LocalDateTime.now());
    }
    if (entry.getActor() == null) {
      entry.setActor(SYSTEM_ACTOR);
    }

    auditLogRepository.save(entry);
    logger.debug(
        "Recorded audit log entry: action={}, actor={}", entry.getAction(), entry.getActor());
  }
}
