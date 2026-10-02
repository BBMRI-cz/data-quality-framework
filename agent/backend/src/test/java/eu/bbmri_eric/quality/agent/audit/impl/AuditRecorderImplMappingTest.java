package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

/** Verifies how {@link AuditRecorderImpl} maps an {@link AuditRecord} onto an audit log entry. */
@ExtendWith(MockitoExtension.class)
class AuditRecorderImplMappingTest {

  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

  @Mock private AuditLogRepository auditLogRepository;

  private ModelMapper modelMapper;
  private AuditRecorderImpl auditRecorder;

  @BeforeEach
  void setUp() {
    modelMapper = new ModelMapper();
    auditRecorder = new AuditRecorderImpl(auditLogRepository, modelMapper);
  }

  @Test
  void record_withAllFields_mapsEachFieldToItsOwnProperty() {
    AuditLogEntry entry =
        recordAndCapture(
            AuditRecord.of(AuditAction.USER_UPDATED)
                .actor("admin", 7L)
                .module("user")
                .entityId(42L)
                .details("Updated user")
                .timestamp(TIMESTAMP)
                .build());

    assertThat(entry.getAction()).isEqualTo(AuditAction.USER_UPDATED);
    assertThat(entry.getActor()).isEqualTo("admin");
    assertThat(entry.getActorId()).isEqualTo(7L);
    assertThat(entry.getModule()).isEqualTo("user");
    assertThat(entry.getEntityId()).isEqualTo(42L);
    assertThat(entry.getDetails()).isEqualTo("Updated user");
    assertThat(entry.getTimestamp()).isEqualTo(TIMESTAMP);
  }

  @Test
  void record_withActorIdAndEntityId_leavesGeneratedIdUnset() {
    AuditLogEntry entry =
        recordAndCapture(
            AuditRecord.of(AuditAction.USER_UPDATED).actor("admin", 7L).entityId(42L).build());

    assertThat(entry.getId()).isNull();
  }

  @Test
  void record_withOnlyActorId_leavesGeneratedIdUnset() {
    AuditLogEntry entry =
        recordAndCapture(AuditRecord.of(AuditAction.LOGOUT).actor("admin", 7L).build());

    assertThat(entry.getId()).isNull();
    assertThat(entry.getActorId()).isEqualTo(7L);
  }

  @Test
  void record_withOnlyEntityId_leavesGeneratedIdUnset() {
    AuditLogEntry entry =
        recordAndCapture(AuditRecord.of(AuditAction.REPORT_CREATED).entityId(42L).build());

    assertThat(entry.getId()).isNull();
    assertThat(entry.getEntityId()).isEqualTo(42L);
  }

  @Test
  void record_withoutTimestamp_defaultsToNow() {
    AuditLogEntry entry = recordAndCapture(AuditRecord.of(AuditAction.AGENT_STARTED).build());

    assertThat(entry.getTimestamp()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
  }

  @Test
  void record_withoutActor_defaultsToSystem() {
    AuditLogEntry entry = recordAndCapture(AuditRecord.of(AuditAction.AGENT_STARTED).build());

    assertThat(entry.getActor()).isEqualTo("SYSTEM");
    assertThat(entry.getActorId()).isNull();
  }

  @Test
  void record_withOnlyAction_leavesOptionalFieldsNull() {
    AuditLogEntry entry = recordAndCapture(AuditRecord.of(AuditAction.AGENT_STOPPED).build());

    assertThat(entry.getAction()).isEqualTo(AuditAction.AGENT_STOPPED);
    assertThat(entry.getModule()).isNull();
    assertThat(entry.getEntityId()).isNull();
    assertThat(entry.getDetails()).isNull();
  }

  @Test
  void constructor_registersStrictTypeMapWithoutChangingSharedConfiguration() {
    assertThat(modelMapper.getTypeMap(AuditRecord.class, AuditLogEntry.class)).isNotNull();
    assertThat(modelMapper.getConfiguration().getMatchingStrategy())
        .isSameAs(MatchingStrategies.STANDARD);
  }

  private AuditLogEntry recordAndCapture(AuditRecord auditRecord) {
    auditRecorder.record(auditRecord);
    ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
    verify(auditLogRepository).save(captor.capture());
    return captor.getValue();
  }
}
