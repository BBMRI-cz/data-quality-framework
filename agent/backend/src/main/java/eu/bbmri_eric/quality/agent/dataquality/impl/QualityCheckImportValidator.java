package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckTransferDTO;
import eu.bbmri_eric.quality.agent.dataquality.exception.QualityCheckImportValidationException;
import eu.bbmri_eric.quality.agent.dataquality.exception.QualityCheckImportValidationException.EntryError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Checks an import file against the existing quality checks before anything is saved. Field-level
 * constraints are handled by bean validation; this validator covers the rules that need the whole
 * file or the database.
 */
@Component
class QualityCheckImportValidator {

  /**
   * Validates all entries and reports every failing one at once.
   *
   * @param entries the import entries, with their type already defaulted
   * @param existingByName the existing checks grouped by name
   * @throws QualityCheckImportValidationException if any entry is invalid
   */
  void validate(
      List<QualityCheckTransferDTO> entries, Map<String, List<QualityCheck>> existingByName) {
    Map<String, Long> occurrences =
        entries.stream()
            .collect(
                Collectors.groupingBy(QualityCheckTransferDTO::getName, Collectors.counting()));
    List<EntryError> errors = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      QualityCheckTransferDTO entry = entries.get(i);
      List<QualityCheck> matches = existingByName.getOrDefault(entry.getName(), List.of());
      int position = i + 1;
      violation(entry, occurrences.get(entry.getName()), matches)
          .ifPresent(reason -> errors.add(new EntryError(position, entry.getName(), reason)));
    }
    if (!errors.isEmpty()) {
      throw new QualityCheckImportValidationException(errors);
    }
  }

  /** Returns the first rule the entry breaks, if any. */
  private Optional<String> violation(
      QualityCheckTransferDTO entry, long occurrences, List<QualityCheck> matches) {
    String name = entry.getName();
    boolean java = entry.getType() == QualityCheckType.JAVA;
    boolean builtIn = matches.size() == 1 && matches.getFirst().getType() == QualityCheckType.JAVA;
    if (occurrences > 1) {
      return Optional.of("the name appears more than once in the file");
    }
    if (matches.size() > 1) {
      return Optional.of("multiple existing checks are named '" + name + "'");
    }
    if (java && !builtIn) {
      return Optional.of(
          "no existing built-in check named '"
              + name
              + "'; JAVA checks can only be updated, never created");
    }
    if (!java && builtIn) {
      return Optional.of(
          "'" + name + "' is a built-in check; its type cannot be changed to " + entry.getType());
    }
    if (!java && (entry.getQuery() == null || entry.getQuery().isBlank())) {
      return Optional.of("query is required for type " + entry.getType());
    }
    return Optional.empty();
  }
}
