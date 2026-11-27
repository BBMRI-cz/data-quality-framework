package eu.bbmri_eric.quality.server.common;

import java.net.URI;

public class TooManyRequestsException extends RuntimeException {

  private final URI type;

  public TooManyRequestsException(String message) {
    this(message, null);
  }

  public TooManyRequestsException(String message, URI type) {
    super(message);
    this.type = type;
  }

  /**
   * Gets the optional ProblemDetail {@code type} URI pointing at troubleshooting documentation for
   * this error, or {@code null} when no specific documentation applies.
   *
   * @return the documentation type URI or null
   */
  public URI getType() {
    return type;
  }
}
