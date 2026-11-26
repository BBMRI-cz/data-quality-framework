package eu.bbmri_eric.quality.agent.dataquality.exception;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Signals that a quality check import file contains invalid entries. It is thrown before anything
 * is saved, so a rejected import never changes the database.
 */
@Getter
public class QualityCheckImportValidationException extends RuntimeException {

  private final transient List<EntryError> errors;

  public QualityCheckImportValidationException(List<EntryError> errors) {
    super(buildMessage(errors));
    this.errors = List.copyOf(errors);
  }

  private static String buildMessage(List<EntryError> errors) {
    return errors.stream()
        .map(error -> "Entry " + error.index() + " '" + error.name() + "': " + error.reason())
        .collect(Collectors.joining("; "));
  }

  /** A single rejected entry of the import file, identified by its position in the file. */
  public record EntryError(int index, String name, String reason) {}
}
