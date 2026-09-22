package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuditServiceImplTest {

  @Autowired private AuditServiceImpl auditService;
  @Autowired private AuditRecorderImpl auditRecorder;
  @Autowired private AuditLogRepository auditLogRepository;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @Test
  void findAll_withoutFilter_returnsAllEntries() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Logged in")
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGOUT).actor("admin", null).details("Logged out").build());

    PageResponse<AuditLogDTO> result = auditService.findAll(new AuditLogFilterDTO());

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent()).hasSize(2);
  }

  @Test
  void findAll_filteredByAction_returnsOnlyMatchingEntries() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Logged in")
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGOUT).actor("admin", null).details("Logged out").build());

    AuditLogFilterDTO filter = new AuditLogFilterDTO();
    filter.setAction(AuditAction.LOGOUT);
    PageResponse<AuditLogDTO> result = auditService.findAll(filter);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getAction()).isEqualTo(AuditAction.LOGOUT);
  }

  @Test
  void findAll_filteredByActor_returnsOnlyMatchingEntries() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Logged in")
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_FAILURE)
            .actor("intruder", null)
            .details("Bad password")
            .build());

    AuditLogFilterDTO filter = new AuditLogFilterDTO();
    filter.setActor("intruder");
    PageResponse<AuditLogDTO> result = auditService.findAll(filter);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getActor()).isEqualTo("intruder");
  }

  @Test
  void findAll_filteredBySearch_matchesDetailsCaseInsensitively() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.SETTINGS_UPDATED)
            .actor("admin", null)
            .details("Changed FHIR URL")
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGOUT).actor("admin", null).details("Logged out").build());

    AuditLogFilterDTO filter = new AuditLogFilterDTO();
    filter.setSearch("fhir");
    PageResponse<AuditLogDTO> result = auditService.findAll(filter);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getDetails()).isEqualTo("Changed FHIR URL");
  }

  @Test
  void findAll_filteredByDateRange_excludesEntriesOutsideRange() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Logged in")
            .build());
    LocalDateTime future = LocalDateTime.now().plusDays(1);

    AuditLogFilterDTO filter = new AuditLogFilterDTO();
    filter.setDateFrom(future);
    PageResponse<AuditLogDTO> result = auditService.findAll(filter);

    assertThat(result.getContent()).isEmpty();
  }
}
