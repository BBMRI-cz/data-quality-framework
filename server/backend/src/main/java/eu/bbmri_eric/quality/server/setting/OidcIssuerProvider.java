package eu.bbmri_eric.quality.server.setting;

/**
 * Provider interface for OIDC issuer URI. Lazily loads the issuer URI from database settings on
 * first access to avoid blocking application startup.
 */
public interface OidcIssuerProvider {

  /**
   * Gets the OIDC issuer URI from database settings. The value is loaded lazily on first access and
   * then cached.
   *
   * @return the OIDC issuer URI, or null if not configured or unavailable
   */
  String getIssuerUri();

  /**
   * Clears the cached issuer URI, forcing a reload on next access. This should be called after OIDC
   * settings are updated in the database.
   */
  void clearCache();
}
