package eu.bbmri_eric.quality.agent.settings.event;

import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import lombok.Getter;

@Getter
public class SettingsUpdatedEvent {
  private final SettingsDTO settings;

  public SettingsUpdatedEvent(SettingsDTO settings) {
    this.settings = settings;
  }
}
