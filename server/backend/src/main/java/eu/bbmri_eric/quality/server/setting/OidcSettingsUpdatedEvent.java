package eu.bbmri_eric.quality.server.setting;

/**
 * Event published when OIDC settings are updated in the database. Listeners can react to this event
 * to refresh their OIDC-related caches and configurations.
 */
public record OidcSettingsUpdatedEvent() {}
