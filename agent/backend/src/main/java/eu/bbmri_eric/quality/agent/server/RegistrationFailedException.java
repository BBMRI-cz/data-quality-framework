package eu.bbmri_eric.quality.agent.server;

import java.util.Objects;
import java.util.OptionalInt;
import org.springframework.http.HttpStatusCode;

/**
 * Thrown when an agent registration with a central server fails.
 *
 * <p>Carries a classified {@link RegistrationFailureReason} and, for failures that happened after
 * an HTTP response was received, the HTTP status of that response. The exception message is the
 * technical detail of the failure: the server's ProblemDetail {@code detail} message when the
 * response provided one, another readable representation otherwise.
 */
public final class RegistrationFailedException extends ServerCommunicationException {

  private final RegistrationFailureReason reason;
  private final Integer httpStatus;

  private RegistrationFailedException(
      RegistrationFailureReason reason, Integer httpStatus, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason cannot be null");
    this.httpStatus = httpStatus;
  }

  /**
   * Creates the exception for a registration that failed with an HTTP error response. The failure
   * reason is derived from the response status.
   *
   * @param status the HTTP status of the error response
   * @param message the technical detail of the failure
   * @param cause the original error, may be null
   * @return the exception
   */
  public static RegistrationFailedException forHttpStatus(
      HttpStatusCode status, String message, Throwable cause) {
    return new RegistrationFailedException(
        RegistrationFailureReason.fromHttpStatus(status), status.value(), message, cause);
  }

  /**
   * Creates the exception for a registration that failed before an HTTP response was received, or
   * for a failure whose reason is known independently of any status code.
   *
   * @param reason the classified failure reason
   * @param message the technical detail of the failure
   * @param cause the original error, may be null
   * @return the exception
   */
  public static RegistrationFailedException forReason(
      RegistrationFailureReason reason, String message, Throwable cause) {
    return new RegistrationFailedException(reason, null, message, cause);
  }

  /**
   * Creates the exception for a registration whose response was accepted but incomplete.
   *
   * @param status the HTTP status of the incomplete response
   * @param missingField the name of the first missing response field
   * @return the exception
   */
  public static RegistrationFailedException incompleteResponse(
      HttpStatusCode status, String missingField) {
    return new RegistrationFailedException(
        RegistrationFailureReason.INCOMPLETE_RESPONSE,
        status.value(),
        "Incomplete registration response: missing %s".formatted(missingField),
        null);
  }

  /**
   * Gets the classified failure reason.
   *
   * @return the failure reason, never null
   */
  public RegistrationFailureReason getReason() {
    return reason;
  }

  /**
   * Gets the HTTP status returned by the central server.
   *
   * @return the status code, or an empty {@link OptionalInt} when the failure happened before an
   *     HTTP response was received
   */
  public OptionalInt getHttpStatus() {
    return httpStatus == null ? OptionalInt.empty() : OptionalInt.of(httpStatus);
  }
}
