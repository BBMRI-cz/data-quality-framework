package eu.bbmri_eric.quality.agent.server;

/**
 * Thrown when communication with a central server fails, e.g. the server is unreachable,
 * authentication fails or the server responds with an unexpected error.
 */
public class ServerCommunicationException extends RuntimeException {

  public ServerCommunicationException(String message) {
    super(message);
  }

  public ServerCommunicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
