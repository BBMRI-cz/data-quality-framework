package eu.bbmri_eric.quality.agent.settings;

import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import jakarta.validation.Valid;

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
   * @param dto the new settings to apply; validated on every call, not only at the REST boundary,
   *     so invalid values are never persisted regardless of the caller
   * @return the updated {@link SettingsDTO}
   */
  SettingsDTO updateSettings(@Valid SettingsDTO dto);
}
