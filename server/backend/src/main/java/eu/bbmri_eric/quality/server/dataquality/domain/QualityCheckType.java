package eu.bbmri_eric.quality.server.dataquality.domain;

/** Represents the type of query used by a quality check. */
public enum QualityCheckType {
  /** The check uses an SQL query. */
  SQL,

  /** The check uses a FHIR query. */
  FHIR
}
