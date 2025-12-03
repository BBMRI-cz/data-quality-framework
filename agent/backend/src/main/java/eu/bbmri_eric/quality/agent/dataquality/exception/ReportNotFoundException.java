package eu.bbmri_eric.quality.agent.dataquality.exception;

/**
 * Exception thrown when a requested report cannot be found in the system.
 *
 * <p>This runtime exception indicates that an operation attempted to retrieve or access a report
 * that does not exist in the database.
 */
public class ReportNotFoundException extends RuntimeException {
  /**
   * Constructs a new ReportNotFoundException with a message containing the report id.
   *
   * @param id the id of the report that was not found
   */
  public ReportNotFoundException(Long id) {
    super("Report not found with id: " + id);
  }
}
