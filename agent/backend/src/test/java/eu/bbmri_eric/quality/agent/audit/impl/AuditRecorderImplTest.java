package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuditRecorderImplTest {

  @Autowired private AuditRecorderImpl auditRecorder;
  @Autowired private AuditLogRepository auditLogRepository;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @Test
  void record_withActor_persistsEntryWithGivenActor() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS).actor("admin", null).details("Logged in").build());

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
    assertThat(entry.getActor()).isEqualTo("admin");
    assertThat(entry.getDetails()).isEqualTo("Logged in");
    assertThat(entry.getTimestamp()).isNotNull();
  }

  @Test
  void record_withNullActor_persistsSystemAsActor() {
    auditRecorder.record(AuditRecord.of(AuditAction.AGENT_STARTED).details("Agent started").build());

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getActor()).isEqualTo("SYSTEM");
  }

  @Test
  void record_withModuleAndEntityId_persistsThem() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.REPORT_CREATED)
            .details("Report created")
            .module("dataquality")
            .entityId(42L)
            .build());

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getModule()).isEqualTo("dataquality");
    assertThat(entry.getEntityId()).isEqualTo(42L);
  }
}