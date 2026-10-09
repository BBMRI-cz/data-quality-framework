package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditService;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import jakarta.persistence.EntityManager;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Service implementation for querying audit log entries. */
@Service
@Transactional(readOnly = true)
class AuditServiceImpl implements AuditService {

  private final AuditLogRepository auditLogRepository;
  private final ModelMapper modelMapper;
  private final EntityManager entityManager;
  private final TransactionTemplate readOnlyTransaction;

  AuditServiceImpl(
      AuditLogRepository auditLogRepository,
      ModelMapper modelMapper,
      EntityManager entityManager,
      PlatformTransactionManager transactionManager) {
    this.auditLogRepository = auditLogRepository;
    this.modelMapper = modelMapper;
    this.entityManager = entityManager;
    this.readOnlyTransaction = new TransactionTemplate(transactionManager);
    this.readOnlyTransaction.setReadOnly(true);
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

  @Override
  public ResponseEntity<StreamingResponseBody> exportCsv(AuditLogFilterDTO filter) {
    StreamingResponseBody body =
        outputStream -> {
          Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
          readOnlyTransaction.executeWithoutResult(status -> writeCsv(filter, writer));
          writer.flush();
        };
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("audit-log-" + LocalDate.now() + ".csv")
                .build()
                .toString())
        .body(body);
  }

  private void writeCsv(AuditLogFilterDTO filter, Writer writer) {
    AuditLogCsvWriter csvWriter = new AuditLogCsvWriter(writer);
    csvWriter.writeHeader();

    try (Stream<AuditLogEntry> entries =
        auditLogRepository.findBy(
            AuditLogSpecification.fromFilter(filter),
            query -> query.sortBy(createSort(filter)).stream())) {
      entries.forEach(
          entry -> {
            csvWriter.write(modelMapper.map(entry, AuditLogDTO.class));
            entityManager.detach(entry);
          });
    }
  }

  private PageRequest createPageRequest(AuditLogFilterDTO filter) {
    return PageRequest.of(filter.getPage(), filter.getSize(), createSort(filter));
  }

  private Sort createSort(AuditLogFilterDTO filter) {
    Sort.Direction direction =
        filter.getOrder() == null || filter.getOrder().name().equalsIgnoreCase("ASC")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    String sortProperty = filter.getSort();
    if (sortProperty == null) {
      sortProperty = "timestamp";
      direction = Sort.Direction.DESC;
    }

    return Sort.by(direction, sortProperty);
  }
}
