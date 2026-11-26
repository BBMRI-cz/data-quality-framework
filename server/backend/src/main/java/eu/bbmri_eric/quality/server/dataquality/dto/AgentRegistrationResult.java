package eu.bbmri_eric.quality.server.dataquality.dto;

/**
 * Internal carrier for a registration outcome: the registration payload plus a flag telling the
 * controller whether an existing agent re-registered from the same IP address (HTTP 200) or a new
 * agent was created (HTTP 201). Never serialized to the wire.
 */
public record AgentRegistrationResult(AgentRegistration registration, boolean reRegistered) {}
