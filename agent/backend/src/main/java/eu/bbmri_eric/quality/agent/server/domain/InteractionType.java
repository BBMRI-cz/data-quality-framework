package eu.bbmri_eric.quality.agent.server.domain;

/**
 * Enumeration representing the type of interaction with a central server.
 *
 * <p>This type indicates the nature of the logged operation performed on or with the server.
 */
public enum InteractionType {
  /** Interaction involving an update operation (e.g., configuration change, status update). */
  UPDATE,

  /**
   * Interaction involving communication with the server (e.g., sending reports, authentication).
   */
  COMMUNICATION,

  /** Interaction involving agent registration with the central server. */
  REGISTRATION,

  /** Interaction involving periodic status check with the central server. */
  STATUS_CHECK
}
