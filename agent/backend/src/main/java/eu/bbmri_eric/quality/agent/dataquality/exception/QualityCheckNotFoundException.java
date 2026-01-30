package eu.bbmri_eric.quality.agent.dataquality.exception;

/** Exception thrown when a quality check is not found. */
public class QualityCheckNotFoundException extends RuntimeException {
  public QualityCheckNotFoundException(Long id) {
    super("Data Quality Check not found with id: " + id);
  }
}
