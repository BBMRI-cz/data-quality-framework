package eu.bbmri_eric.quality.server.setting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of OidcIssuerProvider that lazily loads the issuer URI from database settings on
 * first access to avoid blocking application startup. The issuer URI is cached after first load.
 */
@Component
class OidcIssuerProviderImpl implements OidcIssuerProvider {

  private static final Logger logger = LoggerFactory.getLogger(OidcIssuerProviderImpl.class);

  private final SettingService settingService;
  private volatile String cachedIssuerUri;
  private volatile boolean loaded = false;

  OidcIssuerProviderImpl(SettingService settingService) {
    this.settingService = settingService;
  }

  @Override
  public String getIssuerUri() {
    if (!loaded) {
      synchronized (this) {
        if (!loaded) {
          loadIssuerUri();
          loaded = true;
        }
      }
    }
    return cachedIssuerUri;
  }

  /**
   * Clears the cached issuer URI, forcing a reload on next access. This should be called after OIDC
   * settings are updated in the database.
   */
  @Override
  public void clearCache() {
    synchronized (this) {
      logger.info("Clearing cached OIDC issuer URI");
      cachedIssuerUri = null;
      loaded = false;
    }
  }

  private void loadIssuerUri() {
    try {
      var oidcSettings = settingService.getOidcSettings();
      if (oidcSettings != null
          && oidcSettings.getOidcAuthority() != null
          && !oidcSettings.getOidcAuthority().isBlank()) {
        cachedIssuerUri = oidcSettings.getOidcAuthority();
        logger.info("Loaded OIDC issuer URI from database: {}", cachedIssuerUri);
      } else {
        cachedIssuerUri = null;
        logger.info("OIDC issuer URI not configured in database");
      }
    } catch (Exception e) {
      cachedIssuerUri = null;
      logger.warn("Failed to load OIDC issuer URI from database: {}", e.getMessage());
      logger.debug("Full error details:", e);
    }
  }
}
