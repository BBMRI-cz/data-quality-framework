package eu.bbmri_eric.quality.agent.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Classified reason for a failed agent registration with a central server.
 *
 * <p>Categorizes the low-level failure behind a registration attempt so that logs, persisted
 * interactions and the UI can tell unreachable servers, timeouts, TLS problems and the individual
 * HTTP outcomes apart. Each reason carries a human-readable {@link #summary()} and an actionable
 * {@link #hint()} describing what to check.
 */
public enum RegistrationFailureReason {
  /** The central server could not be reached (DNS failure, connection refused, no route). */
  UNREACHABLE(
      "Cannot reach the central server",
      "Check the server URL and port, that the host name resolves (DNS), and that firewalls,"
          + " proxies or Kubernetes NetworkPolicies allow outgoing connections from the agent."),
  /** The request timed out while connecting or waiting for a response. */
  TIMEOUT(
      "The central server did not respond in time",
      "Check that the central server is running and not overloaded, and the network path or proxy"
          + " between agent and server. Timeouts can be raised with"
          + " agent.central-server.connect-timeout and agent.central-server.read-timeout."),
  /** The TLS handshake with the central server failed. */
  TLS(
      "Secure connection (TLS) to the central server failed",
      "Check that the server certificate is valid and trusted by the agent (custom CA certificates"
          + " must be added to the agent's truststore) and that the URL uses the right scheme (http"
          + " vs https)."),
  /** The server rejected the request as unauthorized or forbidden (HTTP 401/403). */
  UNAUTHORIZED(
      "The central server refused access (HTTP 401/403)",
      "Check whether a reverse proxy or IP allow-list in front of the central server blocks the"
          + " agent, and the server's authentication settings."),
  /** The agent is already registered on the server (HTTP 409). */
  CONFLICT(
      "This agent is already registered on the central server",
      "Ask the central server administrator to remove the existing registration of this agent,"
          + " then remove and add the server here again."),
  /** The server rate-limited the registration request (HTTP 429). */
  RATE_LIMITED(
      "The central server is rate-limiting registration requests",
      "Wait a few minutes before trying again. The server may also allow only one agent"
          + " registration per IP address; ask its administrator if this persists."),
  /** The server responded with another client error (other 4xx status). */
  CLIENT_ERROR(
      "The central server rejected the registration request",
      "Check that the URL points to the DQF central server (not another service) and that agent"
          + " and server versions are compatible."),
  /** The server responded with a server error (HTTP 5xx status). */
  SERVER_ERROR(
      "The central server reported an internal error",
      "Check the central server's logs, then try again later."),
  /** The server accepted the request but returned an incomplete registration response. */
  INCOMPLETE_RESPONSE(
      "The central server sent an incomplete registration response",
      "Check that the URL points to the DQF central server (not a proxy or another service) and"
          + " that agent and server versions are compatible."),
  /** The failure could not be classified. */
  UNKNOWN(
      "Registration failed for an unexpected reason",
      "See the technical detail below and the agent log for the full stack trace.");

  private final String summary;
  private final String hint;

  RegistrationFailureReason(String summary, String hint) {
    this.summary = summary;
    this.hint = hint;
  }

  /**
   * Gets the short human-readable description of this failure reason.
   *
   * @return the failure summary
   */
  public String summary() {
    return summary;
  }

  /**
   * Gets the actionable advice on what to check for this failure reason.
   *
   * @return the troubleshooting hint
   */
  public String hint() {
    return hint;
  }

  /**
   * Gets whether a registration that failed with this reason may be retried automatically. Only
   * transient failures (connectivity problems, timeouts, TLS handshake failures, server errors and
   * rate limiting) are retryable; failures that require a configuration change or administrative
   * action are not.
   *
   * @return true when an automatic retry makes sense, false otherwise
   */
  public boolean isRetryable() {
    return this == UNREACHABLE
        || this == TIMEOUT
        || this == TLS
        || this == SERVER_ERROR
        || this == RATE_LIMITED;
  }

  /**
   * Maps the HTTP status of a failed registration response to the corresponding failure reason.
   *
   * @param status the HTTP status returned by the central server
   * @return the failure reason this status implies, {@link #UNKNOWN} for statuses without a
   *     specific meaning for registration
   */
  public static RegistrationFailureReason fromHttpStatus(HttpStatusCode status) {
    if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED) || status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
      return UNAUTHORIZED;
    }
    if (status.isSameCodeAs(HttpStatus.CONFLICT)) {
      return CONFLICT;
    }
    if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
      return RATE_LIMITED;
    }
    if (status.is5xxServerError()) {
      return SERVER_ERROR;
    }
    if (status.is4xxClientError()) {
      return CLIENT_ERROR;
    }
    return UNKNOWN;
  }
}
