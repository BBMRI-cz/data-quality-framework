package eu.bbmri_eric.quality.agent.settings.event;

import eu.bbmri_eric.quality.agent.settings.dto.DiffPrivacySettingsDTO;
import lombok.Getter;

@Getter
public class DiffPrivacySettingsUpdateEvent {
  private final DiffPrivacySettingsDTO updatedDiffPrivacySettings;

  public DiffPrivacySettingsUpdateEvent(DiffPrivacySettingsDTO dto) {
    this.updatedDiffPrivacySettings = dto;
  }
}
