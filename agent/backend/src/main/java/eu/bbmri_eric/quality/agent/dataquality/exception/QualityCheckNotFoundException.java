package eu.bbmri_eric.quality.agent.dataquality.exception;

import eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException;

/** Exception thrown when a quality check is not found. */
public class QualityCheckNotFoundException extends EntityNotFoundException {
  public QualityCheckNotFoundException(Long id) {
    super("Data Quality Check not found with id: " + id);
  }
}
