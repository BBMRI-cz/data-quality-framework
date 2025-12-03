package eu.bbmri_eric.quality.agent.common.exception;

/**
 * Exception thrown when a requested entity cannot be found in the system.
 *
 * <p>This runtime exception indicates that an operation attempted to retrieve or access an entity
 * that does not exist in the database or system.
 */
public class EntityNotFoundException extends RuntimeException {
  /**
   * Constructs a new EntityNotFoundException with the specified detail message.
   *
   * @param message the detail message explaining which entity was not found
   */
  public EntityNotFoundException(String message) {
    super(message);
  }

  /** Constructs a new EntityNotFoundException with a default message. */
  public EntityNotFoundException() {
    super("Entity not found");
  }
}
