package eu.bbmri_eric.quality.agent.server.domain;

/**
 * Lifecycle state of this agent's registration with a central server.
 *
 * <p>Describes the registration itself; {@link ServerConnectionStatus} stays separate and keeps
 * describing reachability and approval, so "never registered" and "registered but unreachable" do
 * not share a single failure state.
 */
public enum RegistrationStatus {
  /** No registration has been attempted or claimed for this server. */
  UNREGISTERED,

  /** A registration run has been dispatched and has not persisted an outcome yet. */
  REGISTERING,

  /** The central server accepted the registration; administrator approval is pending. */
  REGISTERED_PENDING_APPROVAL,

  /** Registered and approved by the central server; derived at read time, never persisted. */
  ACTIVE,

  /**
   * The last registration run failed; the classified reason and message are recorded separately.
   */
  FAILED
}
