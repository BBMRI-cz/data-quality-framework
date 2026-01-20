package eu.bbmri_eric.quality.server.setting;

public interface SettingService {
  SettingDTO getSettings();

  OidcSettingsDTO getOidcSettings();

  SettingDTO updateSettings(SettingDTO settingDTO);

  OidcSettingsDTO updateOidcSettings(OidcSettingsDTO oidcSettingsDTO);
}
