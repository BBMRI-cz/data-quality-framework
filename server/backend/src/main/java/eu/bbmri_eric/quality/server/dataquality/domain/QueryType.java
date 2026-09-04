package eu.bbmri_eric.quality.server.dataquality.domain;

/** Type of query language used by a {@link QualityCheckVersion}. */
public enum QueryType {
  CQL,
  SQL,
  PYTHON,
  UNKNOWN,
}
