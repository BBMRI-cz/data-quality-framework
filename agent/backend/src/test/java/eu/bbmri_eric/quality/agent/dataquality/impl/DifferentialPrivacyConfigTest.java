package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DifferentialPrivacyConfigTest {

  @Test
  void updateConfig_withProvidedValues_shouldUseProvidedValues() {
    DifferentialPrivacyConfig config = new DifferentialPrivacyConfig();
    SettingsDTO settings = new SettingsDTO();
    settings.setEpsilon(2.5);
    settings.setDelta(1.0E-7);
    settings.setMinThreshold(25);
    settings.setNoiseMechanism(NoiseMechanism.LAPLACE);

    ReflectionTestUtils.invokeMethod(config, "updateConfig", new SettingsUpdatedEvent(settings));

    assertEquals(2.5, config.getEpsilon());
    assertEquals(1.0E-7, config.getDelta());
    assertEquals(25, config.getMinThreshold());
    assertEquals(NoiseMechanism.LAPLACE, config.getNoiseMechanism());
  }

  @Test
  void updateConfig_withNullDifferentialPrivacyValues_shouldUseDefaults() {
    DifferentialPrivacyConfig config = new DifferentialPrivacyConfig();
    SettingsDTO settings = new SettingsDTO();

    ReflectionTestUtils.invokeMethod(config, "updateConfig", new SettingsUpdatedEvent(settings));

    assertEquals(3.0, config.getEpsilon());
    assertEquals(1.0E-8, config.getDelta());
    assertEquals(50, config.getMinThreshold());
    assertEquals(NoiseMechanism.GAUSSIAN, config.getNoiseMechanism());
  }
}
