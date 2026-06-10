package eu.bbmri_eric.quality.server.dataquality;

/** Service interface for generating PDF report summaries. */
public interface ReportPdfService {

  /**
   * Generates a PDF summary for the given report.
   *
   * @param reportId the report ID
   * @return the generated PDF as a byte array
   */
  byte[] generateReportSummary(String reportId);
}
