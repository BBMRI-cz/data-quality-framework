package eu.bbmri_eric.quality.agent.dataquality.domain;

/** Represents the type of quality check implementation. */
public enum QualityCheckType {
  /** Clinical Quality Language based check with a CQL query. */
  CQL,

  /** Java-based built-in check implementation. */
  JAVA
}
