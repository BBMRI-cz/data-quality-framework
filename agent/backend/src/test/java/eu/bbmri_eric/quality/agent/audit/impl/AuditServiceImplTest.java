package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.AuditService;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
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

  @Autowired private AuditService auditService;
  @Autowired private AuditLogRepository auditLogRepository;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @Test
  void record_withActor_persistsEntryWithGivenActor() {
    auditService.record(AuditAction.LOGIN_SUCCESS, "admin", "Logged in");

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
    assertThat(entry.getActor()).isEqualTo("admin");
    assertThat(entry.getDetails()).isEqualTo("Logged in");
    assertThat(entry.getTimestamp()).isNotNull();
  }

  @Test
  void record_withNullActor_persistsSystemAsActor() {
    auditService.record(AuditAction.AGENT_STARTED, null, "Agent started");

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getActor()).isEqualTo("SYSTEM");
  }

  @Test
  void record_withModuleAndEntityId_persistsThem() {
    auditService.record(AuditAction.REPORT_CREATED, null, "Report created", "dataquality", 42L);

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getModule()).isEqualTo("dataquality");
    assertThat(entry.getEntityId()).isEqualTo(42L);
  }

  @Test
  void findAll_withoutFilter_returnsAllEntries() {
    auditService.record(AuditAction.LOGIN_SUCCESS, "admin", "Logged in");
    auditService.record(AuditAction.LOGOUT, "admin", "Logged out");

    PageResponse<AuditLogDTO> result = auditService.findAll(new AuditLogFilterDTO());

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent()).hasSize(2);
  }

  @Test
  void findAll_filteredByAction_returnsOnlyMatchingEntries() {
    auditService.record(AuditAction.LOGIN_SUCCESS, "admin", "Logged in");
    auditService.record(AuditAction.LOGOUT, "admin", "Logged out");

    AuditLogFilterDTO filter = new AuditLogFilterDTO();
    filter.setAction(AuditAction.LOGOUT);
    PageResponse<AuditLogDTO> result = auditService.findAll(filter);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getAction()).isEqualTo(AuditAction.LOGOUT);
  }

  @Test
  void findAll_filteredByActor_returnsOnlyMatchingEntries() {
    auditService.record(AuditAction.LOGIN_SUCCESS, "admin", "Logged in");
    auditService.record(AuditAction.LOGIN_FAILURE, "intruder", "Bad password");

    AuditLogFilterDTO filter = new AuditLogFilterDTO();
    filter.setActor("intruder");
    PageResponse<AuditLogDTO> result = auditService.findAll(filter);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getActor()).isEqualTo("intruder");
  }

  @Test
  void findAll_filteredBySearch_matchesDetailsCaseInsensitively() {
    auditService.record(AuditAction.SETTINGS_UPDATED, "admin", "Changed FHIR URL");
    auditService.record(AuditAction.LOGOUT, "admin", "Logged out");

    AuditLogFilterDTO filter = new AuditLogFilterDTO();
    filter.setSearch("fhir");
    PageResponse<AuditLogDTO> result = auditService.findAll(filter);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getDetails()).isEqualTo("Changed FHIR URL");
  }

  @Test
  void findAll_filteredByDateRange_excludesEntriesOutsideRange() {
    auditService.record(AuditAction.LOGIN_SUCCESS, "admin", "Logged in");
    LocalDateTime future = LocalDateTime.now().plusDays(1);

    AuditLogFilterDTO filter = new AuditLogFilterDTO();
    filter.setDateFrom(future);
    PageResponse<AuditLogDTO> result = auditService.findAll(filter);

    assertThat(result.getContent()).isEmpty();
  }
}
