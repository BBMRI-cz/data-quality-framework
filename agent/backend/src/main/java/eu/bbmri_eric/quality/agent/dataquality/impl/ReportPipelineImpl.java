package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.ReportPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Orchestrates report generation. The pipeline itself is intentionally not transactional; the
 * transactional boundaries live in {@link ReportPipelineExecutor} so that a failed report can
 * always be marked as FAILED, even when a step poisoned the inner transaction.
 */
@Slf4j
@Component
class ReportPipelineImpl implements ReportPipeline {

  private final ReportPipelineExecutor executor;

  ReportPipelineImpl(ReportPipelineExecutor executor) {
    this.executor = executor;
  }

  @Override
  public void run(Long reportId) {
    log.info("Starting report pipeline for report id: {}", reportId);
    try {
      executor.runSteps(reportId);
    } catch (Exception e) {
      log.error("Report pipeline failed for report id: {}", reportId, e);
      executor.markFailed(reportId);
      return;
    }
    log.info("Completed report pipeline for report id: {}", reportId);
  }
}
