package eu.bbmri_eric.quality.agent.dataquality;

/**
 * Pipeline for processing reports through multiple steps. Orchestrates the execution of report
 * generation steps in sequence.
 */
public interface ReportPipeline {

  /**
   * Runs the pipeline on the report with the given id, executing all steps in order.
   *
   * @param reportId the id of the report to process
   */
  void run(Long reportId);
}
