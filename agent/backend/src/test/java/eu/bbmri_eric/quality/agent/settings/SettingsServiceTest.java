package eu.bbmri_eric.quality.agent.settings;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.impl.SettingsRepository;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SettingsServiceTest {

  @Autowired private SettingsService settingsService;

  @Autowired private SettingsRepository settingsRepository;

  @BeforeEach
  void setUp() {
    settingsRepository.deleteAll();
    settingsRepository.save(new Settings("fhirUrl", "http://localhost:8080/fhir"));
    settingsRepository.save(new Settings("fhirUsername", "testuser"));
    settingsRepository.save(new Settings("fhirPassword", "dGVzdHBhc3M="));
    settingsRepository.save(new Settings("agentId", "sdfsdf-sdgsfgdfg-dfgdfg"));
  }

  @Test
  void updateSettings_shouldPublishEvent() {
    String base64Password = Base64.getEncoder().encodeToString("eventpass".getBytes());
    SettingsDTO dto = new SettingsDTO("http://localhost:8080/fhir", "eventuser", base64Password);

    assertDoesNotThrow(() -> settingsService.updateSettings(dto));
  }

  @Test
  void updateSettings_shouldThrowException_whenSettingNotFound() {
    settingsRepository.deleteById("fhirUrl");

    String base64Password = Base64.getEncoder().encodeToString("password".getBytes());
    SettingsDTO dto = new SettingsDTO("http://localhost:8080/fhir", "user", base64Password);

    assertThrows(IllegalStateException.class, () -> settingsService.updateSettings(dto));
  }

  @Test
  void getSettings_afterUpdate_shouldReturnUpdatedValues() {
    String newUrl = "http://production:8080/fhir";
    String newUsername = "produser";
    String newPassword = "prodpass123";
    String base64Password = Base64.getEncoder().encodeToString(newPassword.getBytes());

    SettingsDTO updateDto = new SettingsDTO(newUrl, newUsername, base64Password);
    settingsService.updateSettings(updateDto);

    SettingsDTO result = settingsService.getSettings();

    assertEquals(newUrl, result.getFhirUrl());
    assertEquals(newUsername, result.getFhirUsername());

    assertEquals(base64Password, result.getFhirPassword());

    String decodedPassword = new String(Base64.getDecoder().decode(result.getFhirPassword()));
    assertEquals(newPassword, decodedPassword);
  }

  @Test
  void updateSettings_shouldThrowException_whenInvalidSettingNameProvided() {
    settingsRepository.deleteById("fhirUsername");

    String base64Password = Base64.getEncoder().encodeToString("password".getBytes());
    SettingsDTO dto = new SettingsDTO("http://localhost:8080/fhir", "user", base64Password);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> settingsService.updateSettings(dto));
    assertTrue(exception.getMessage().contains("Setting not found"));
    assertTrue(exception.getMessage().contains("fhirUsername"));
  }
}
