package eu.bbmri_eric.quality.agent.server;

/**
 * Classified reason for a failed agent registration with a central server.
 *
 * <p>Categorizes the low-level failure behind a registration attempt so that logs, persisted
 * interactions and the UI can tell unreachable servers, timeouts, TLS problems and the individual
 * HTTP outcomes apart.
 */
public enum RegistrationFailureReason {
  /** The central server could not be reached (DNS failure, connection refused, no route). */
  UNREACHABLE,
  /** The request timed out while connecting or waiting for a response. */
  TIMEOUT,
  /** The TLS handshake with the central server failed. */
  TLS,
  /** The server rejected the request as unauthorized or forbidden (HTTP 401/403). */
  UNAUTHORIZED,
  /** The agent is already registered on the server (HTTP 409). */
  CONFLICT,
  /** The server rate-limited the registration request (HTTP 429). */
  RATE_LIMITED,
  /** The server responded with another client error (other 4xx status). */
  CLIENT_ERROR,
  /** The server responded with a server error (HTTP 5xx status). */
  SERVER_ERROR,
  /** The server accepted the request but returned an incomplete registration response. */
  INCOMPLETE_RESPONSE,
  /** The failure could not be classified. */
  UNKNOWN
}
