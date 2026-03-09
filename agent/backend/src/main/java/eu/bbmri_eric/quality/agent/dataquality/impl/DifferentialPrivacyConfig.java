package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.event.DiffPrivacySettingsUpdateEvent;
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
  private void updateConfig(DiffPrivacySettingsUpdateEvent event) {
    this.epsilon = event.getUpdatedDiffPrivacySettings().getEpsilon();
    this.delta = event.getUpdatedDiffPrivacySettings().getDelta();
    this.minThreshold = event.getUpdatedDiffPrivacySettings().getMinThreshold();
    this.noiseMechanism = event.getUpdatedDiffPrivacySettings().getNoiseMechanism();
  }
}
