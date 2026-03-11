package eu.bbmri_eric.quality.agent.settings.event;

import eu.bbmri_eric.quality.agent.settings.dto.FhirSettingsDTO;

public class FhirSettingsUpdateEvent {
  private final FhirSettingsDTO updatedSettings;

  public FhirSettingsUpdateEvent(FhirSettingsDTO settings) {
    this.updatedSettings = settings;
  }

  public FhirSettingsDTO getSettings() {
    return updatedSettings;
  }
}
