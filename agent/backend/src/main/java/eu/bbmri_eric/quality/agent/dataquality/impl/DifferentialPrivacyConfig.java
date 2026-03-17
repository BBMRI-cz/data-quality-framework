package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Getter
class DifferentialPrivacyConfig {
  private static final double DEFAULT_EPSILON = 3.0;
  private static final double DEFAULT_DELTA = 1.0E-8;
  private static final int DEFAULT_MIN_THRESHOLD = 50;
  private static final NoiseMechanism DEFAULT_NOISE_MECHANISM = NoiseMechanism.GAUSSIAN;

  private double epsilon;
  private double delta;
  private int minThreshold;
  private NoiseMechanism noiseMechanism;

  public DifferentialPrivacyConfig() {}

  @EventListener
  private void updateConfig(SettingsUpdatedEvent event) {
    SettingsDTO settings = event.getSettings();
    this.epsilon = settings.getEpsilon() != null ? settings.getEpsilon() : DEFAULT_EPSILON;
    this.delta = settings.getDelta() != null ? settings.getDelta() : DEFAULT_DELTA;
    this.minThreshold =
        settings.getMinThreshold() != null ? settings.getMinThreshold() : DEFAULT_MIN_THRESHOLD;
    this.noiseMechanism =
        settings.getNoiseMechanism() != null
            ? settings.getNoiseMechanism()
            : DEFAULT_NOISE_MECHANISM;
  }
}
