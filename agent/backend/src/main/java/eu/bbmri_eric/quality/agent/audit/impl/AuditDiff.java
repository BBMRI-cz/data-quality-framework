package eu.bbmri_eric.quality.agent.audit.impl;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Describes the differences between two snapshots of a state as {@code name from old to new} pairs,
 * e.g. {@code epsilon from 1.0 to 0.5; fhirPassword changed}.
 */
final class AuditDiff {

  private static final String EMPTY_VALUE = "(empty)";
  private static final String SENSITIVE_CHANGE = "changed";
  private static final String ALWAYS_SENSITIVE = "password";

  private AuditDiff() {}

  /**
   * @param before the snapshot taken before the change
   * @param after the snapshot taken after the change
   * @param sensitive properties whose values must be hidden
   * @return the changed properties, in snapshot order, or an empty string if nothing changed
   */
  static String describe(Map<String, ?> before, Map<String, ?> after, Set<String> sensitive) {
    Set<String> names = new LinkedHashSet<>(before.keySet());
    names.addAll(after.keySet());
    return names.stream()
        .filter(name -> !Objects.equals(normalize(before.get(name)), normalize(after.get(name))))
        .map(name -> describe(name, before.get(name), after.get(name), sensitive))
        .collect(Collectors.joining("; "));
  }

  private static String describe(String name, Object before, Object after, Set<String> sensitive) {
    if (isSensitive(name, sensitive)) {
      return name + " " + SENSITIVE_CHANGE;
    }
    return name + " from " + display(before) + " to " + display(after);
  }

  private static boolean isSensitive(String name, Set<String> sensitive) {
    return sensitive.contains(name) || name.toLowerCase(Locale.ROOT).contains(ALWAYS_SENSITIVE);
  }

  /**
   * Treats a blank value the same as a missing one, so {@code null} to {@code ""} is not a change.
   */
  private static Object normalize(Object value) {
    return value instanceof String text && text.isEmpty() ? null : value;
  }

  private static String display(Object value) {
    Object normalized = normalize(value);
    return normalized == null ? EMPTY_VALUE : normalized.toString();
  }
}
