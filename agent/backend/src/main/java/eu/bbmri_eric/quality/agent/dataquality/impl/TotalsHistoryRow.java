package eu.bbmri_eric.quality.agent.dataquality.impl;

/**
 * One scalar row of the entity-total history, returned by {@link
 * ReportRepository#findTotalsHistory} — a typed alternative to an untyped object array. Contains no
 * entities and no patient identifiers.
 */
record TotalsHistoryRow(
    Integer numberOfEntities,
    Integer numberOfSecondaryEntities,
    Integer obfuscatedNumberOfEntities,
    Integer obfuscatedNumberOfSecondaryEntities) {}
