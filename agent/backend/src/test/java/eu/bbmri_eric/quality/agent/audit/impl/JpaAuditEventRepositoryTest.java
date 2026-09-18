package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpaAuditEventRepositoryTest {

  @Autowired private AuditEventRepository auditEventRepository;
  @Autowired private AuditLogRepository auditLogRepository;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @Test
  void add_withKnownActionType_persistsMatchingAction() {
    auditEventRepository.add(
        new AuditEvent(
            "admin", AuditAction.SETTINGS_UPDATED.name(), Map.of("details", "Changed FHIR URL")));

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getAction()).isEqualTo(AuditAction.SETTINGS_UPDATED);
    assertThat(entry.getActor()).isEqualTo("admin");
    assertThat(entry.getDetails()).isEqualTo("Changed FHIR URL");
  }

  @Test
  void add_withAuthenticationSuccessType_mapsToLoginSuccess() {
    auditEventRepository.add(new AuditEvent("admin", "AUTHENTICATION_SUCCESS", Map.of()));

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
  }

  @Test
  void add_withAuthenticationFailureType_mapsToLoginFailure() {
    auditEventRepository.add(new AuditEvent("admin", "AUTHENTICATION_FAILURE", Map.of()));

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_FAILURE);
  }

  @Test
  void add_withUnknownType_mapsToOther() {
    auditEventRepository.add(new AuditEvent("admin", "SOME_UNRECOGNISED_TYPE", Map.of()));

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getAction()).isEqualTo(AuditAction.OTHER);
  }

  @Test
  void add_withModuleAndEntityId_persistsThem() {
    auditEventRepository.add(
        new AuditEvent(
            "admin",
            AuditAction.REPORT_CREATED.name(),
            Map.of("module", "dataquality", "entityId", 42L)));

    AuditLogEntry entry = auditLogRepository.findAll().getFirst();
    assertThat(entry.getModule()).isEqualTo("dataquality");
    assertThat(entry.getEntityId()).isEqualTo(42L);
  }

  @Test
  void find_filtersByPrincipalAfterAndType() {
    auditEventRepository.add(
        new AuditEvent(
            Instant.now().minusSeconds(60), "admin", AuditAction.LOGOUT.name(), Map.of()));
    auditEventRepository.add(new AuditEvent("admin", AuditAction.LOGIN_SUCCESS.name(), Map.of()));
    auditEventRepository.add(
        new AuditEvent("intruder", AuditAction.LOGIN_FAILURE.name(), Map.of()));

    List<AuditEvent> events =
        auditEventRepository.find(
            "admin", Instant.now().minusSeconds(30), AuditAction.LOGIN_SUCCESS.name());

    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getPrincipal()).isEqualTo("admin");
    assertThat(events.getFirst().getType()).isEqualTo(AuditAction.LOGIN_SUCCESS.name());
  }
}
