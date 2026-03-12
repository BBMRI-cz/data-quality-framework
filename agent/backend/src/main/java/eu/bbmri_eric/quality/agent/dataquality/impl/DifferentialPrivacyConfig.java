package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Getter
public class DifferentialPrivacyConfig {
  private double epsilon;
  private double delta;
  private int minThreshold;
  private NoiseMechanism noiseMechanism;

  public DifferentialPrivacyConfig() {}

  @EventListener
  private void updateConfig(SettingsUpdatedEvent event) {
    this.epsilon = event.getSettings().getEpsilon();
    this.delta = event.getSettings().getDelta();
    this.minThreshold = event.getSettings().getMinThreshold();
    this.noiseMechanism = event.getSettings().getNoiseMechanism();
  }
}
