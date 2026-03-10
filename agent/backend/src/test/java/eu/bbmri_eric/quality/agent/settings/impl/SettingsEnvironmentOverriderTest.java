package eu.bbmri_eric.quality.agent.settings.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class SettingsEnvironmentOverriderTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Mock private SettingsService settingsService;
  @Mock private ApplicationArguments args;
  private Map<String, String> testEnv;
  private SettingsEnvironmentOverrider overrider;

  @BeforeEach
  void setUp() {
    testEnv = new HashMap<>();
    overrider =
        new SettingsEnvironmentOverrider(settingsService, objectMapper) {
          @Override
          protected Map<String, String> getSystemEnv() {
            return testEnv;
          }
        };
  }

  @Test
  void run_shouldOverrideSettings_whenEnvironmentVariableExists() {
    SettingsDTO initialSettings = new SettingsDTO();
    initialSettings.setFhirUrl("http://old-url.com");
    initialSettings.setFhirUsername("oldUser");
    when(settingsService.getSettings()).thenReturn(initialSettings);
    testEnv.put("APP_SETTING_FHIR_URL", "http://new-url.com");
    overrider.run(args);
    ArgumentCaptor<SettingsDTO> captor = ArgumentCaptor.forClass(SettingsDTO.class);
    verify(settingsService).updateSettings(captor.capture());
    SettingsDTO updatedSettings = captor.getValue();
    assertEquals("http://new-url.com", updatedSettings.getFhirUrl());
    assertEquals("oldUser", updatedSettings.getFhirUsername());
  }

  @Test
  void run_shouldNotUpdateSettings_whenValueIsSame() {
    SettingsDTO initialSettings = new SettingsDTO();
    initialSettings.setFhirUrl("http://same-url.com");
    when(settingsService.getSettings()).thenReturn(initialSettings);
    testEnv.put("APP_SETTING_FHIR_URL", "http://same-url.com");
    overrider.run(args);
    verify(settingsService, never()).updateSettings(any());
  }

  @Test
  void run_shouldIgnoreEnvironmentVariablesWithoutPrefix() {
    SettingsDTO initialSettings = new SettingsDTO();
    initialSettings.setFhirUrl("http://old-url.com");
    when(settingsService.getSettings()).thenReturn(initialSettings);
    testEnv.put("FHIR_URL", "http://ignored-url.com");
    overrider.run(args);
    verify(settingsService, never()).updateSettings(any());
  }

  @Test
  void run_shouldHandleCaseIsensitiveMatching() {
    SettingsDTO initialSettings = new SettingsDTO();
    initialSettings.setFhirUsername("oldUser");
    when(settingsService.getSettings()).thenReturn(initialSettings);
    testEnv.put("APP_SETTING_FHIRUSERNAME", "newUser");
    overrider.run(args);
    ArgumentCaptor<SettingsDTO> captor = ArgumentCaptor.forClass(SettingsDTO.class);
    verify(settingsService).updateSettings(captor.capture());
    assertEquals("newUser", captor.getValue().getFhirUsername());
  }

  @Test
  void run_shouldNormalizeUnderscoresAndDots() {
    SettingsDTO initialSettings = new SettingsDTO();
    initialSettings.setFhirUrl("http://old-url.com");
    when(settingsService.getSettings()).thenReturn(initialSettings);
    testEnv.put("APP_SETTING_FHIR.URL", "http://new-url.com");
    overrider.run(args);
    ArgumentCaptor<SettingsDTO> captor = ArgumentCaptor.forClass(SettingsDTO.class);
    verify(settingsService).updateSettings(captor.capture());
    assertEquals("http://new-url.com", captor.getValue().getFhirUrl());
  }

  @Test
  void run_shouldHandleDuplicateNormalizedKeysDeterministically() {
    SettingsDTO initialSettings = new SettingsDTO();
    initialSettings.setFhirUrl("http://old-url.com");
    when(settingsService.getSettings()).thenReturn(initialSettings);
    testEnv.put("APP_SETTING_FHIRURL", "http://winner.com");
    testEnv.put("APP_SETTING_FHIR_URL", "http://loser.com");
    overrider.run(args);
    ArgumentCaptor<SettingsDTO> captor = ArgumentCaptor.forClass(SettingsDTO.class);
    verify(settingsService).updateSettings(captor.capture());
    assertEquals("http://winner.com", captor.getValue().getFhirUrl());
  }
}
