package eu.bbmri_eric.quality.agent.settings;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.dto.DiffPrivacySettingsDTO;
import eu.bbmri_eric.quality.agent.settings.dto.FhirSettingsDTO;
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

    settingsRepository.save(new Settings("epsilon", "1.0"));
    settingsRepository.save(new Settings("delta", "1.0E-8"));
    settingsRepository.save(new Settings("minThreshold", "10"));
    settingsRepository.save(new Settings("noiseMechanism", "LAPLACE"));
  }

  @Test
  void updateSettings_shouldPublishEvent() {
    String base64Password = Base64.getEncoder().encodeToString("eventpass".getBytes());
    FhirSettingsDTO dto =
        new FhirSettingsDTO("http://localhost:8080/fhir", "eventuser", base64Password);

    assertDoesNotThrow(() -> settingsService.updateSettings(dto));
  }

  @Test
  void updateSettings_shouldThrowException_whenSettingNotFound() {
    settingsRepository.deleteById("fhirUrl");

    String base64Password = Base64.getEncoder().encodeToString("password".getBytes());
    FhirSettingsDTO dto = new FhirSettingsDTO("http://localhost:8080/fhir", "user", base64Password);

    assertThrows(IllegalStateException.class, () -> settingsService.updateSettings(dto));
  }

  @Test
  void getSettings_afterUpdate_shouldReturnUpdatedValues() {
    String newUrl = "http://production:8080/fhir";
    String newUsername = "produser";
    String newPassword = "prodpass123";
    String base64Password = Base64.getEncoder().encodeToString(newPassword.getBytes());

    FhirSettingsDTO updateDto = new FhirSettingsDTO(newUrl, newUsername, base64Password);
    settingsService.updateFhirSettings(updateDto);

    FhirSettingsDTO result = settingsService.getFhirSettings();

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
    FhirSettingsDTO dto = new FhirSettingsDTO("http://localhost:8080/fhir", "user", base64Password);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> settingsService.updateFhirSettings(dto));
    assertTrue(exception.getMessage().contains("Setting not found"));
    assertTrue(exception.getMessage().contains("fhirUsername"));
  }

  @Test
  void getDiffPrivacySettings_shouldReturnCurrentSettings() {
    DiffPrivacySettingsDTO result = settingsService.getDiffPrivacySettings();

    assertNotNull(result);
    assertEquals(1.0, result.getEpsilon());
    assertEquals(1.0E-8, result.getDelta());
    assertEquals(10, result.getMinThreshold());
    assertEquals(NoiseMechanism.LAPLACE, result.getNoiseMechanism());
  }

  @Test
  void updateDiffPrivacySettings_shouldUpdateAllFields() {
    DiffPrivacySettingsDTO updateDto =
        new DiffPrivacySettingsDTO(3.0, 1.0E-10, 50, NoiseMechanism.GAUSSIAN);

    DiffPrivacySettingsDTO result = settingsService.updateDiffPrivacySettings(updateDto);

    assertNotNull(result);
    assertEquals(3.0, result.getEpsilon());
    assertEquals(1.0E-10, result.getDelta());
    assertEquals(50, result.getMinThreshold());
    assertEquals(NoiseMechanism.GAUSSIAN, result.getNoiseMechanism());
  }

  @Test
  void getDiffPrivacySettings_afterUpdate_shouldReturnUpdatedValues() {
    DiffPrivacySettingsDTO updateDto =
        new DiffPrivacySettingsDTO(2.5, 1.0E-9, 25, NoiseMechanism.GAUSSIAN);

    settingsService.updateDiffPrivacySettings(updateDto);
    DiffPrivacySettingsDTO result = settingsService.getDiffPrivacySettings();

    assertEquals(2.5, result.getEpsilon());
    assertEquals(1.0E-9, result.getDelta());
    assertEquals(25, result.getMinThreshold());
    assertEquals(NoiseMechanism.GAUSSIAN, result.getNoiseMechanism());
  }

  @Test
  void updateDiffPrivacySettings_shouldPublishEvent() {
    DiffPrivacySettingsDTO dto =
        new DiffPrivacySettingsDTO(1.5, 1.0E-8, 15, NoiseMechanism.LAPLACE);

    assertDoesNotThrow(() -> settingsService.updateDiffPrivacySettings(dto));
  }

  @Test
  void updateDiffPrivacySettings_shouldThrowException_whenSettingNotFound() {
    settingsRepository.deleteById("epsilon");

    DiffPrivacySettingsDTO dto =
        new DiffPrivacySettingsDTO(2.0, 1.0E-8, 20, NoiseMechanism.LAPLACE);

    assertThrows(IllegalStateException.class, () -> settingsService.updateDiffPrivacySettings(dto));
  }

  @Test
  void updateDiffPrivacySettings_withLaplaceNoiseMechanism_shouldPersist() {
    DiffPrivacySettingsDTO updateDto =
        new DiffPrivacySettingsDTO(1.0, 1.0E-8, 10, NoiseMechanism.LAPLACE);

    settingsService.updateDiffPrivacySettings(updateDto);
    DiffPrivacySettingsDTO result = settingsService.getDiffPrivacySettings();

    assertEquals(NoiseMechanism.LAPLACE, result.getNoiseMechanism());
    assertEquals("LAPLACE", settingsRepository.findById("noiseMechanism").get().getValue());
  }

  @Test
  void updateDiffPrivacySettings_withZeroMinThreshold_shouldSucceed() {
    DiffPrivacySettingsDTO updateDto =
        new DiffPrivacySettingsDTO(1.0, 1.0E-8, 0, NoiseMechanism.LAPLACE);

    DiffPrivacySettingsDTO result = settingsService.updateDiffPrivacySettings(updateDto);

    assertEquals(0, result.getMinThreshold());
  }

  @Test
  void updateDiffPrivacySettings_withVerySmallDelta_shouldSucceed() {
    DiffPrivacySettingsDTO updateDto =
        new DiffPrivacySettingsDTO(1.0, 1.0E-15, 10, NoiseMechanism.GAUSSIAN);

    DiffPrivacySettingsDTO result = settingsService.updateDiffPrivacySettings(updateDto);

    assertEquals(1.0E-15, result.getDelta());
  }
}
