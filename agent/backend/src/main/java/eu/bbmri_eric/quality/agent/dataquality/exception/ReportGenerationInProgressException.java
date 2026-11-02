package eu.bbmri_eric.quality.agent.dataquality.exception;

/**
 * Exception thrown when a new report is requested while another report is still being generated.
 *
 * <p>Only one report may be generated at a time
 */
public class ReportGenerationInProgressException extends RuntimeException {
  /**
   * Constructs a new ReportGenerationInProgressException containing the id of the report currently
   * being generated.
   *
   * @param id the id of the report that is still in the GENERATING state
   */
  public ReportGenerationInProgressException(Long id) {
    super("Report generation already in progress (report id: %s)".formatted(id));
  }
}
