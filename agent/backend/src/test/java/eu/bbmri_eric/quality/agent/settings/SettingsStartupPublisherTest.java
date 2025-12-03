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

  @Test
  void publishSettingsOnStartup_shouldPublishSettingsUpdatedEvent() {
    SettingsDTO mockSettings =
        new SettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    when(settingsService.getSettings()).thenReturn(mockSettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    ArgumentCaptor<SettingsUpdatedEvent> eventCaptor =
        ArgumentCaptor.forClass(SettingsUpdatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    SettingsUpdatedEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent);
    assertEquals(mockSettings, capturedEvent.getSettings());
  }

  @Test
  void publishSettingsOnStartup_shouldCallSettingsService() {
    SettingsDTO mockSettings =
        new SettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    when(settingsService.getSettings()).thenReturn(mockSettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    verify(settingsService, times(1)).getSettings();
  }

  @Test
  void publishSettingsOnStartup_shouldPublishEventWithCorrectSource() {
    SettingsDTO mockSettings =
        new SettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    when(settingsService.getSettings()).thenReturn(mockSettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    ArgumentCaptor<SettingsUpdatedEvent> eventCaptor =
        ArgumentCaptor.forClass(SettingsUpdatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
  }
}
