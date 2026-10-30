package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.privacy.NoiseHistory;

/**
 * One scalar row of the published check-result history, returned by {@link
 * ReportRepository#findResultHistory} — a typed alternative to an untyped object array. Contains no
 * entities and no patient identifiers.
 */
record ResultHistoryRow(
    Long checkId, String stratum, Integer rawValue, Double obfuscatedValue, Double epsilon) {

  NoiseHistory.HistoryEntry toHistoryEntry() {
    double published = obfuscatedValue == null ? Double.NaN : obfuscatedValue;
    return new NoiseHistory.HistoryEntry(rawValue, published, epsilon);
  }
}
