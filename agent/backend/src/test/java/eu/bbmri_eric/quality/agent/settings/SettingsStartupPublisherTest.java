package eu.bbmri_eric.quality.agent.settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.settings.dto.DiffPrivacySettingsDTO;
import eu.bbmri_eric.quality.agent.settings.dto.FhirSettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.DiffPrivacySettingsUpdateEvent;
import eu.bbmri_eric.quality.agent.settings.event.FhirSettingsUpdateEvent;
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
    FhirSettingsDTO mockSettings =
        new FhirSettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    when(settingsService.getFhirSettings()).thenReturn(mockSettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    ArgumentCaptor<FhirSettingsUpdateEvent> eventCaptor =
        ArgumentCaptor.forClass(FhirSettingsUpdateEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    FhirSettingsUpdateEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent);
    assertEquals(mockSettings, capturedEvent.getSettings());
  }

  @Test
  void publishSettingsOnStartup_shouldCallSettingsService() {
    FhirSettingsDTO mockSettings =
        new FhirSettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    when(settingsService.getFhirSettings()).thenReturn(mockSettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    verify(settingsService, times(1)).getFhirSettings();
  }

  @Test
  void publishSettingsOnStartup_shouldPublishEventWithCorrectSource() {
    FhirSettingsDTO mockSettings =
        new FhirSettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    when(settingsService.getFhirSettings()).thenReturn(mockSettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    ArgumentCaptor<FhirSettingsUpdateEvent> eventCaptor =
        ArgumentCaptor.forClass(FhirSettingsUpdateEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
  }

  @Test
  void publishSettingsOnStartup_shouldCallGetDiffPrivacySettings() {
    FhirSettingsDTO mockSettings =
        new FhirSettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    DiffPrivacySettingsDTO mockDiffPrivacySettings =
        new DiffPrivacySettingsDTO(1.0, 1.0E-8, 10, NoiseMechanism.LAPLACE);

    when(settingsService.getFhirSettings()).thenReturn(mockSettings);
    when(settingsService.getDiffPrivacySettings()).thenReturn(mockDiffPrivacySettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    verify(settingsService, times(1)).getDiffPrivacySettings();
  }

  @Test
  void publishSettingsOnStartup_shouldPublishDiffPrivacySettingsUpdateEvent() {
    FhirSettingsDTO mockSettings =
        new FhirSettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    DiffPrivacySettingsDTO mockDiffPrivacySettings =
        new DiffPrivacySettingsDTO(2.0, 1.0E-9, 20, NoiseMechanism.GAUSSIAN);

    when(settingsService.getFhirSettings()).thenReturn(mockSettings);
    when(settingsService.getDiffPrivacySettings()).thenReturn(mockDiffPrivacySettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    ArgumentCaptor<DiffPrivacySettingsUpdateEvent> eventCaptor =
        ArgumentCaptor.forClass(DiffPrivacySettingsUpdateEvent.class);
    verify(eventPublisher, times(2)).publishEvent(any());
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    DiffPrivacySettingsUpdateEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent);
    assertNotNull(capturedEvent.getUpdatedDiffPrivacySettings());
    assertEquals(mockDiffPrivacySettings, capturedEvent.getUpdatedDiffPrivacySettings());
  }

  @Test
  void publishSettingsOnStartup_shouldPublishDiffPrivacyEventWithCorrectParameters() {
    FhirSettingsDTO mockSettings =
        new FhirSettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");
    DiffPrivacySettingsDTO mockDiffPrivacySettings =
        new DiffPrivacySettingsDTO(3.0, 1.0E-7, 50, NoiseMechanism.GAUSSIAN);

    when(settingsService.getFhirSettings()).thenReturn(mockSettings);
    when(settingsService.getDiffPrivacySettings()).thenReturn(mockDiffPrivacySettings);

    settingsStartupPublisher.publishSettingsOnStartup();

    ArgumentCaptor<DiffPrivacySettingsUpdateEvent> eventCaptor =
        ArgumentCaptor.forClass(DiffPrivacySettingsUpdateEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    DiffPrivacySettingsUpdateEvent capturedEvent = eventCaptor.getValue();
    DiffPrivacySettingsDTO capturedSettings = capturedEvent.getUpdatedDiffPrivacySettings();

    assertEquals(3.0, capturedSettings.getEpsilon());
    assertEquals(1.0E-7, capturedSettings.getDelta());
    assertEquals(50, capturedSettings.getMinThreshold());
    assertEquals(NoiseMechanism.GAUSSIAN, capturedSettings.getNoiseMechanism());
  }
}
