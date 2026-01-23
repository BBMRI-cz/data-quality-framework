package eu.bbmri_eric.quality.server.setting;

/** Service interface for managing application settings. */
public interface SettingService {

  /**
   * Retrieves all application settings.
   *
   * @return SettingDTO containing current application configuration
   */
  SettingDTO getSettings();

  /**
   * Retrieves OIDC configuration settings. This method returns the OIDC configuration needed for
   * frontend authentication, including authority URL, client ID, redirect URIs, and OAuth scopes.
   * The settings are used to initialize the OIDC client in the frontend application.
   *
   * @return OidcSettingsDTO containing current OIDC configuration
   */
  OidcSettingsDTO getOidcSettings();

  /**
   * Updates general application settings.
   *
   * @param settingDTO the settings to update, must not be null and must be valid
   * @return SettingDTO containing the updated application settings
   */
  SettingDTO updateSettings(SettingDTO settingDTO);

  /**
   * Updates OIDC (OpenID Connect) configuration settings.
   *
   * <p>This method allows updating the OIDC settings used for frontend authentication, such as
   * authority URL, client ID, redirect URIs, and OAuth scopes.
   *
   * @param oidcSettingsDTO the OIDC settings to update, must not be null and must be valid
   * @return OidcSettingsDTO containing the updated OIDC configuration
   */
  OidcSettingsDTO updateOidcSettings(OidcSettingsDTO oidcSettingsDTO);
}
