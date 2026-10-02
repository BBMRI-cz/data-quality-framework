package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import java.time.LocalDateTime;
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
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Logged in")
            .build());

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
    assertThat(entry.getActor()).isEqualTo("admin");
    assertThat(entry.getDetails()).isEqualTo("Logged in");
    assertThat(entry.getTimestamp()).isNotNull();
  }

  @Test
  void record_withNullActor_persistsSystemAsActor() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.AGENT_STARTED).details("Agent started").build());

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

  @Test
  void record_withAllFields_mapsEachToItsOwnColumn() {
    LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
    auditRecorder.record(
        AuditRecord.of(AuditAction.USER_UPDATED)
            .actor("admin", 7L)
            .module("user")
            .entityId(42L)
            .details("Updated user")
            .timestamp(timestamp)
            .build());

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getAction()).isEqualTo(AuditAction.USER_UPDATED);
    assertThat(entry.getActor()).isEqualTo("admin");
    assertThat(entry.getActorId()).isEqualTo(7L);
    assertThat(entry.getModule()).isEqualTo("user");
    assertThat(entry.getEntityId()).isEqualTo(42L);
    assertThat(entry.getDetails()).isEqualTo("Updated user");
    assertThat(entry.getTimestamp()).isEqualTo(timestamp);
  }

  @Test
  void record_twice_createsTwoEntries() {
    auditRecorder.record(AuditRecord.of(AuditAction.LOGIN_SUCCESS).actor("admin", 7L).build());
    auditRecorder.record(AuditRecord.of(AuditAction.LOGOUT).actor("admin", 7L).build());

    assertThat(auditLogRepository.findAll()).hasSize(2);
  }
}
