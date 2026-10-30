package eu.bbmri_eric.quality.agent.dataquality.impl;

/**
 * One scalar row of the published check-result history, returned by {@link
 * ReportRepository#findResultHistory} — a typed alternative to an untyped object array. Contains no
 * entities and no patient identifiers.
 */
record ResultHistoryRow(
    Long checkId, String stratum, Integer rawValue, Double obfuscatedValue, Double epsilon) {

  NoiseAnchors.HistoryEntry<Double> toHistoryEntry() {
    return new NoiseAnchors.HistoryEntry<>(rawValue, obfuscatedValue, epsilon);
  }
}
