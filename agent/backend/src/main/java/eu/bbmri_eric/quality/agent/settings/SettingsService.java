package eu.bbmri_eric.quality.agent.settings;

import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;

public interface SettingsService {
  SettingsDTO getSettings();

  SettingsDTO updateSettings(SettingsDTO dto);
}
