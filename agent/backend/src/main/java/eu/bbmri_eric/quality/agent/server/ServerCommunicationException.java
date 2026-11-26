package eu.bbmri_eric.quality.agent.server;

/**
 * Thrown when communication with a central server fails, e.g. the server is unreachable,
 * authentication fails or the server responds with an unexpected error.
 *
 * <p>Registration failures additionally carry a {@link RegistrationFailureReason}, the HTTP status
 * and the server's ProblemDetail {@code detail} message when they are known.
 */
public class ServerCommunicationException extends RuntimeException {

  private final RegistrationFailureReason reason;
  private final Integer httpStatus;
  private final String detail;

  /**
   * Constructs an unclassified communication failure.
   *
   * @param message the human-readable failure message
   */
  public ServerCommunicationException(String message) {
    this(message, null, null, null, null);
  }

  /**
   * Constructs an unclassified communication failure with a cause.
   *
   * @param message the human-readable failure message
   * @param cause the original error
   */
  public ServerCommunicationException(String message, Throwable cause) {
    this(message, null, null, null, cause);
  }

  /**
   * Constructs a classified communication failure.
   *
   * @param message the human-readable failure message
   * @param reason the classified failure reason, may be null
   * @param httpStatus the HTTP status returned by the server, may be null
   * @param detail the server's ProblemDetail detail message, may be null
   * @param cause the original error, may be null
   */
  public ServerCommunicationException(
      String message,
      RegistrationFailureReason reason,
      Integer httpStatus,
      String detail,
      Throwable cause) {
    super(message, cause);
    this.reason = reason;
    this.httpStatus = httpStatus;
    this.detail = detail;
  }

  /**
   * Gets the classified failure reason.
   *
   * @return the reason, or null when the failure is not classified
   */
  public RegistrationFailureReason getReason() {
    return reason;
  }

  /**
   * Gets the HTTP status returned by the central server.
   *
   * @return the status code, or null when the failure happened before an HTTP response
   */
  public Integer getHttpStatus() {
    return httpStatus;
  }

  /**
   * Gets the server's RFC-7807 ProblemDetail {@code detail} message.
   *
   * @return the detail text, or null when the server did not provide a parseable one
   */
  public String getDetail() {
    return detail;
  }
}
