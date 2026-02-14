package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.domain.Report;

/**
 * A step in the report generation pipeline. Each step performs a specific transformation on the
 * report entity.
 */
public interface ReportPipelineStep {

  /**
   * Executes this pipeline step on the given report.
   *
   * @param report the report to process
   * @return the processed report
   */
  Report execute(Report report);

  /**
   * Returns the order in which this step should be executed. Lower values execute first.
   *
   * @return the step order
   */
  int getOrder();
}
