package eu.bbmri_eric.quality.agent.dataquality.privacy;

/**
 * One row of a published-count history, newest first. A {@code null} {@code raw} or {@code
 * published} marks an entry where nothing usable was recorded (e.g. a check that failed that day);
 * replay skips such entries without breaking the chain.
 */
public record HistoryEntry(Integer raw, Double published, Double epsilon) {}
