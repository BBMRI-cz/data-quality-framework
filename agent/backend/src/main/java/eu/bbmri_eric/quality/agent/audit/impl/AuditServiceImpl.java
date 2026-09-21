package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.AuditService;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for recording and querying audit log entries. */
@Service
@Transactional
class AuditServiceImpl implements AuditService, AuditRecorder {

  private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
  private static final String SYSTEM_ACTOR = "SYSTEM";

  private final AuditLogRepository auditLogRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ModelMapper modelMapper;

  AuditServiceImpl(
      AuditLogRepository auditLogRepository,
      ApplicationEventPublisher eventPublisher,
      ModelMapper modelMapper) {
    this.auditLogRepository = auditLogRepository;
    this.eventPublisher = eventPublisher;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional
  public void record(AuditAction action, String actor, String details) {
    record(action, actor, details, null, null);
  }

  @Override
  @Transactional
  public void record(
      AuditAction action, String actor, String details, String module, Long entityId) {
    record(action, actor, null, details, module, entityId);
  }

  @Override
  @Transactional
  public void record(
      AuditAction action,
      String actor,
      Long actorId,
      String details,
      String module,
      Long entityId) {
    String principal = actor != null ? actor : SYSTEM_ACTOR;

    Map<String, Object> data = new LinkedHashMap<>();
    if (details != null) {
      data.put("details", details);
    }
    if (module != null) {
      data.put("module", module);
    }
    if (entityId != null) {
      data.put("entityId", entityId);
    }
    if (actorId != null) {
      data.put("actorId", actorId);
    }

    eventPublisher.publishEvent(
        new AuditApplicationEvent(new AuditEvent(principal, action.name(), data)));
    logger.debug("Recorded audit log entry: action={}, actor={}", action, principal);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<AuditLogDTO> findAll(AuditLogFilterDTO filter) {
    PageRequest pageRequest = createPageRequest(filter);
    Page<AuditLogEntry> page =
        auditLogRepository.findAll(AuditLogSpecification.fromFilter(filter), pageRequest);
    List<AuditLogDTO> content =
        page.getContent().stream().map(entry -> modelMapper.map(entry, AuditLogDTO.class)).toList();
    return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
  }

  private PageRequest createPageRequest(AuditLogFilterDTO filter) {
    Sort.Direction direction =
        filter.getOrder() == null || filter.getOrder().name().equalsIgnoreCase("ASC")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    String sortProperty = filter.getSort();
    if (sortProperty == null) {
      sortProperty = "timestamp";
      direction = Sort.Direction.DESC;
    }

    Sort sort = Sort.by(direction, sortProperty);
    return PageRequest.of(filter.getPage(), filter.getSize(), sort);
  }
}
