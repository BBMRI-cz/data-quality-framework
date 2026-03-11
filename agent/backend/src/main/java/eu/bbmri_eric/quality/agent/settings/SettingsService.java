package eu.bbmri_eric.quality.agent.settings;

import eu.bbmri_eric.quality.agent.settings.dto.DiffPrivacySettingsDTO;
import eu.bbmri_eric.quality.agent.settings.dto.FhirSettingsDTO;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;

public interface SettingsService {
  SettingsDTO getSettings();

  SettingsDTO updateSettings(SettingsDTO dto);

  FhirSettingsDTO getFhirSettings();

  FhirSettingsDTO updateFhirSettings(FhirSettingsDTO dto);

  DiffPrivacySettingsDTO getDiffPrivacySettings();

  DiffPrivacySettingsDTO updateDiffPrivacySettings(DiffPrivacySettingsDTO dto);
}
