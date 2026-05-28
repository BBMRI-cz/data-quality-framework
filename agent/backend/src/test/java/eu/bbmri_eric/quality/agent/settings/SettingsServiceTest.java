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
    settingsRepository.save(new Settings("epsilon", "1.0"));
    settingsRepository.save(new Settings("delta", "1.0E-8"));
    settingsRepository.save(new Settings("minThreshold", "10"));
    settingsRepository.save(new Settings("noiseMechanism", "LAPLACE"));
  }

  private SettingsDTO createSettingsDTO(
      String fhirUrl,
      String fhirUsername,
      String fhirPassword,
      double epsilon,
      double delta,
      int minThreshold,
      NoiseMechanism noiseMechanism) {
    return SettingsDTO.builder()
        .fhirUrl(fhirUrl)
        .fhirUsername(fhirUsername)
        .fhirPassword(fhirPassword)
        .epsilon(epsilon)
        .delta(delta)
        .minThreshold(minThreshold)
        .noiseMechanism(noiseMechanism)
        .build();
  }

  @Test
  void getSettings_shouldReturnAllFields() {
    SettingsDTO result = settingsService.getSettings();

    assertNotNull(result);
    assertEquals("http://localhost:8080/fhir", result.getFhirUrl());
    assertEquals("testuser", result.getFhirUsername());
    assertEquals(1.0, result.getEpsilon());
    assertEquals(1.0E-8, result.getDelta());
    assertEquals(10, result.getMinThreshold());
    assertEquals(NoiseMechanism.LAPLACE, result.getNoiseMechanism());
  }

  @Test
  void updateSettings_shouldPersistAllFields() {
    String base64Password = Base64.getEncoder().encodeToString("newpass".getBytes());
    SettingsDTO dto =
        createSettingsDTO(
            "http://production:8080/fhir",
            "produser",
            base64Password,
            0.5,
            1.0E-10,
            50,
            NoiseMechanism.GAUSSIAN);

    settingsService.updateSettings(dto);
    SettingsDTO result = settingsService.getSettings();

    assertEquals("http://production:8080/fhir", result.getFhirUrl());
    assertEquals("produser", result.getFhirUsername());
    assertEquals(base64Password, result.getFhirPassword());
    assertEquals(0.5, result.getEpsilon());
    assertEquals(1.0E-10, result.getDelta());
    assertEquals(50, result.getMinThreshold());
    assertEquals(NoiseMechanism.GAUSSIAN, result.getNoiseMechanism());
  }

  @Test
  void updateSettings_shouldPublishEvents_withoutThrowing() {
    SettingsDTO dto =
        createSettingsDTO(
            "http://localhost:8080/fhir",
            "eventuser",
            Base64.getEncoder().encodeToString("eventpass".getBytes()),
            1.5,
            1.0E-8,
            15,
            NoiseMechanism.LAPLACE);

    assertDoesNotThrow(() -> settingsService.updateSettings(dto));
  }

  @Test
  void updateSettings_shouldThrowException_whenSettingNotFound() {
    settingsRepository.deleteById("fhirUrl");

    SettingsDTO dto =
        createSettingsDTO(
            "http://localhost:8080/fhir",
            "user",
            Base64.getEncoder().encodeToString("password".getBytes()),
            1.0,
            1.0E-8,
            10,
            NoiseMechanism.LAPLACE);

    assertThrows(IllegalStateException.class, () -> settingsService.updateSettings(dto));
  }

  @Test
  void updateSettings_shouldThrowException_withMessageContainingMissingKey() {
    settingsRepository.deleteById("fhirUsername");

    SettingsDTO dto =
        createSettingsDTO(
            "http://localhost:8080/fhir",
            "user",
            Base64.getEncoder().encodeToString("password".getBytes()),
            1.0,
            1.0E-8,
            10,
            NoiseMechanism.LAPLACE);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> settingsService.updateSettings(dto));
    assertTrue(exception.getMessage().contains("Setting not found"));
    assertTrue(exception.getMessage().contains("fhirUsername"));
  }

  @Test
  void updateSettings_withLaplaceNoiseMechanism_shouldPersist() {
    SettingsDTO dto =
        createSettingsDTO(
            "http://localhost:8080/fhir",
            "testuser",
            "dGVzdHBhc3M=",
            1.0,
            1.0E-8,
            10,
            NoiseMechanism.LAPLACE);

    settingsService.updateSettings(dto);

    assertEquals("LAPLACE", settingsRepository.findById("noiseMechanism").get().getValue());
  }

  @Test
  void updateSettings_withZeroMinThreshold_shouldSucceed() {
    SettingsDTO dto =
        createSettingsDTO(
            "http://localhost:8080/fhir",
            "testuser",
            "dGVzdHBhc3M=",
            1.0,
            1.0E-8,
            0,
            NoiseMechanism.LAPLACE);

    SettingsDTO result = settingsService.updateSettings(dto);

    assertEquals(0, result.getMinThreshold());
  }

  @Test
  void updateSettings_withVerySmallDelta_shouldSucceed() {
    SettingsDTO dto =
        createSettingsDTO(
            "http://localhost:8080/fhir",
            "testuser",
            "dGVzdHBhc3M=",
            1.0,
            1.0E-15,
            10,
            NoiseMechanism.GAUSSIAN);

    SettingsDTO result = settingsService.updateSettings(dto);

    assertEquals(1.0E-15, result.getDelta());
  }

  @Test
  void updateSettings_shouldThrowException_whenEpsilonSettingNotFound() {
    settingsRepository.deleteById("epsilon");

    SettingsDTO dto =
        createSettingsDTO(
            "http://localhost:8080/fhir",
            "testuser",
            "dGVzdHBhc3M=",
            2.0,
            1.0E-8,
            20,
            NoiseMechanism.LAPLACE);

    assertThrows(IllegalStateException.class, () -> settingsService.updateSettings(dto));
  }

  @Test
  void updateSettings_shouldThrowException_whenDbGaussianAndEpsilonAboveOne() {
    SettingsDTO first =
        createSettingsDTO(
            "http://localhost:8080/fhir",
            "testuser",
            "dGVzdHBhc3M=",
            0.5,
            1.0E-8,
            10,
            NoiseMechanism.GAUSSIAN);
    settingsService.updateSettings(first);

    SettingsDTO second = SettingsDTO.builder().epsilon(2.0).build();

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> settingsService.updateSettings(second));
    assertTrue(
        exception
            .getMessage()
            .contains("Epsilon must be less than or equal to 1.0 when using Gaussian noise"));
  }
}
