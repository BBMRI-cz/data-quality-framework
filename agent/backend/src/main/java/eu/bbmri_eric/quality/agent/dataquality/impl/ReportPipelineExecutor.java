package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes report pipeline steps and handles report failure transitions, each in its own
 * transaction.
 *
 * <p>The step loop runs in its own (inner) transaction: if a step fails through any transactional
 * bean, that transaction is marked rollback-only and discarded whole, so the failure handler must
 * not join it. {@link #markFailed(Long)} therefore uses {@code REQUIRES_NEW} to mark the report as
 * {@link ReportStatus#FAILED} in a separate, fresh transaction that cannot be rolled back by the
 * poisoned one.
 */
@Slf4j
@Component
class ReportPipelineExecutor {

  private final ReportRepository reportRepository;
  private final List<ReportPipelineStep> steps;

  ReportPipelineExecutor(ReportRepository reportRepository, List<ReportPipelineStep> steps) {
    this.reportRepository = reportRepository;
    this.steps =
        steps.stream().sorted(Comparator.comparingInt(ReportPipelineStep::getOrder)).toList();
  }

  @Transactional
  public void runSteps(Long reportId) {
    Report report =
        reportRepository
            .findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
    for (ReportPipelineStep step : steps) {
      log.debug("Executing pipeline step: {}", step.getClass().getSimpleName());
      report = step.execute(report);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(Long reportId) {
    reportRepository
        .findById(reportId)
        .ifPresentOrElse(
            report -> {
              report.setStatus(ReportStatus.FAILED);
              reportRepository.save(report);
              log.info("Marked report id: {} as FAILED", reportId);
            },
            () -> log.warn("Cannot mark missing report id: {} as FAILED", reportId));
  }
}
