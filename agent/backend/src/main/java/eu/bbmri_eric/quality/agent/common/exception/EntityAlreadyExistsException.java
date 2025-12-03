package eu.bbmri_eric.quality.agent.common.exception;

/**
 * Exception thrown when an entity already exists in the system.
 *
 * <p>This runtime exception indicates that an operation attempted to create or add an entity that
 * violates uniqueness constraints or already exists in the database.
 */
public class EntityAlreadyExistsException extends RuntimeException {
  /**
   * Constructs a new EntityAlreadyExistsException with the specified detail message.
   *
   * @param message the detail message explaining why the entity already exists
   */
  public EntityAlreadyExistsException(String message) {
    super(message);
  }
}
