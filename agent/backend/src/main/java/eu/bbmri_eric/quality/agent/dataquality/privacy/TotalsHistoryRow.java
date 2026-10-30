package eu.bbmri_eric.quality.agent.dataquality.privacy;

/**
 * One scalar row of the entity-total history, returned by the totals projection query — a typed
 * alternative to an untyped object array. Contains no entities and no patient identifiers.
 */
public record TotalsHistoryRow(
    Integer numberOfEntities,
    Integer numberOfSecondaryEntities,
    Integer obfuscatedNumberOfEntities,
    Integer obfuscatedNumberOfSecondaryEntities) {}
