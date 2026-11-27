package eu.bbmri_eric.quality.server.common;

import java.net.URI;

public class EntityAlreadyExistsException extends RuntimeException {

  private final URI type;

  public EntityAlreadyExistsException(String message) {
    this(message, null);
  }

  public EntityAlreadyExistsException(String message, URI type) {
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
