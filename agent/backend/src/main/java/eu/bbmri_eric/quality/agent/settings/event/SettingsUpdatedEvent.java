package eu.bbmri_eric.quality.agent.settings.event;

import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;

public class SettingsUpdatedEvent {
  private final SettingsDTO updatedSettings;

  public SettingsUpdatedEvent(SettingsDTO settings) {
    this.updatedSettings = settings;
  }

  public SettingsDTO getSettings() {
    return updatedSettings;
  }
}
