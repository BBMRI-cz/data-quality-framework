package eu.bbmri_eric.quality.agent.settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import eu.bbmri_eric.quality.agent.settings.impl.SettingsStartupPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsStartupPublisherTest {

  @Mock private SettingsService settingsService;
  @Mock private EventPublisher eventPublisher;

  private SettingsStartupPublisher settingsStartupPublisher;

  @BeforeEach
  void setUp() {
    settingsStartupPublisher = new SettingsStartupPublisher(settingsService, eventPublisher);
  }

  private SettingsDTO mockSettings() {
    SettingsDTO dto = new SettingsDTO();
    dto.setFhirUrl("http://localhost:8080/fhir");
    dto.setFhirUsername("testuser");
    dto.setFhirPassword("dGVzdHBhc3M=");
    dto.setEpsilon(2.0);
    dto.setDelta(1.0E-9);
    dto.setMinThreshold(20);
    dto.setNoiseMechanism(NoiseMechanism.GAUSSIAN);
    return dto;
  }

  @Test
  void publishSettingsOnStartup_shouldCallGetSettingsOnce() {
    when(settingsService.getSettings()).thenReturn(mockSettings());

    settingsStartupPublisher.publishSettingsOnStartup();

    verify(settingsService, times(1)).getSettings();
  }

  @Test
  void publishSettingsOnStartup_shouldPublishSettingsUpdatedEvent() {
    SettingsDTO settings = mockSettings();
    when(settingsService.getSettings()).thenReturn(settings);

    settingsStartupPublisher.publishSettingsOnStartup();

    ArgumentCaptor<SettingsUpdatedEvent> captor =
        ArgumentCaptor.forClass(SettingsUpdatedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertNotNull(captor.getValue());
    assertEquals(settings, captor.getValue().getSettings());
  }

  @Test
  void publishSettingsOnStartup_shouldPublishOneEvent() {
    when(settingsService.getSettings()).thenReturn(mockSettings());

    settingsStartupPublisher.publishSettingsOnStartup();

    verify(eventPublisher, times(1)).publishEvent(any());
  }

  @Test
  void publishSettingsOnStartup_shouldPublishEventWithCorrectParameters() {
    SettingsDTO settings = mockSettings();
    when(settingsService.getSettings()).thenReturn(settings);

    settingsStartupPublisher.publishSettingsOnStartup();

    ArgumentCaptor<SettingsUpdatedEvent> captor =
        ArgumentCaptor.forClass(SettingsUpdatedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());

    SettingsDTO captured = captor.getValue().getSettings();
    assertEquals("http://localhost:8080/fhir", captured.getFhirUrl());
    assertEquals("testuser", captured.getFhirUsername());
    assertEquals(2.0, captured.getEpsilon());
    assertEquals(1.0E-9, captured.getDelta());
    assertEquals(20, captured.getMinThreshold());
    assertEquals(NoiseMechanism.GAUSSIAN, captured.getNoiseMechanism());
  }
}
