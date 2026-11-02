package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.domain.Report;

/**
 * A step in the report generation pipeline. Each step performs a specific transformation on the
 * report entity. Step order is declared once here (see the ORDER constants) instead of being spread
 * as magic numbers across the step classes.
 */
public interface ReportPipelineStep {

  /** Runs the quality checks (CQL/SQL/Python/built-in) and collects raw results. */
  int QUALITY_CHECKS_ORDER = 10;

  /** Counts entities in the backing data store. */
  int ENTITY_COUNT_ORDER = 20;

  /** Adds differential-privacy noise to results and entity totals. */
  int OBFUSCATION_ORDER = 30;

  /** Marks the report as generated and publishes the generation event. */
  int FINISHING_ORDER = 40;

  /**
   * Executes this pipeline step on the given report.
   *
   * @param report the report to process
   * @return the processed report
   */
  Report execute(Report report);

  /**
   * Returns the order in which this step should be executed, one of {@code *_ORDER} above. Lower
   * values execute first.
   *
   * @return the step order
   */
  int getOrder();
}
