package eu.bbmri_eric.quality.agent.dataquality.privacy;

/**
 * One scalar row of the published check-result history, returned by the result-history projection
 * query — a typed alternative to an untyped object array. Contains no entities and no patient
 * identifiers.
 */
public record ResultHistoryRow(
    Long checkId, String stratum, Integer rawValue, Double obfuscatedValue, Double epsilon) {

  public HistoryEntry toHistoryEntry() {
    return new HistoryEntry(rawValue, obfuscatedValue, epsilon);
  }
}
