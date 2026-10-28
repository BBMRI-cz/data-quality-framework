package eu.bbmri_eric.quality.server.dataquality;

/**
 * Service interface for exporting reports in the W3C Data Quality Vocabulary (DQV).
 *
 * <p>The export is read-only: it renders an existing report as RDF/Turtle without changing how
 * reports are stored or ingested. Query texts (CQL/SQL/...) are never serialized; only their hash
 * and language are included.
 */
public interface ReportDqvService {

  /**
   * Exports a report as an RDF/Turtle document modeled with the W3C Data Quality Vocabulary.
   *
   * @param reportId the report ID
   * @return the Turtle serialization of the report
   */
  String exportDqv(String reportId);
}
