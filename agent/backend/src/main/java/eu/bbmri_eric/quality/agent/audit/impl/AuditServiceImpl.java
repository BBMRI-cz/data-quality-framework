package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditService;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for querying audit log entries. */
@Service
@Transactional(readOnly = true)
class AuditServiceImpl implements AuditService {

  private final AuditLogRepository auditLogRepository;
  private final ModelMapper modelMapper;

  AuditServiceImpl(AuditLogRepository auditLogRepository, ModelMapper modelMapper) {
    this.auditLogRepository = auditLogRepository;
    this.modelMapper = modelMapper;
  }

  @Override
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