package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditService;
import eu.bbmri_eric.quality.agent.audit.domain.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for recording and querying audit log entries. */
@Service
@Transactional
class AuditServiceImpl implements AuditService {

  private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
  private static final String SYSTEM_ACTOR = "SYSTEM";

  private final AuditLogRepository auditLogRepository;
  private final ModelMapper modelMapper;

  AuditServiceImpl(AuditLogRepository auditLogRepository, ModelMapper modelMapper) {
    this.auditLogRepository = auditLogRepository;
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
    AuditLogEntry entry = new AuditLogEntry();
    entry.setTimestamp(LocalDateTime.now());
    entry.setActor(actor != null ? actor : SYSTEM_ACTOR);
    entry.setAction(action);
    entry.setDetails(details);
    entry.setModule(module);
    entry.setEntityId(entityId);
    auditLogRepository.save(entry);
    logger.debug("Recorded audit log entry: action={}, actor={}", action, entry.getActor());
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