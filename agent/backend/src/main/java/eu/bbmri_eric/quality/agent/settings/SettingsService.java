package eu.bbmri_eric.quality.agent.settings;

import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;

/** Service interface for managing application settings. */
public interface SettingsService {

  /**
   * Returns the current application settings.
   *
   * @return current {@link SettingsDTO}
   */
  SettingsDTO getSettings();

  /**
   * Updates the application settings and publishes relevant events.
   *
   * @param dto the new settings to apply
   * @return the updated {@link SettingsDTO}
   */
  SettingsDTO updateSettings(SettingsDTO dto);
}
