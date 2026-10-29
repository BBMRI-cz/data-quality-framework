package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.ReportPipeline;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
class ReportPipelineImpl implements ReportPipeline {

  private final ReportRepository reportRepository;
  private final List<ReportPipelineStep> steps;

  ReportPipelineImpl(ReportRepository reportRepository, List<ReportPipelineStep> steps) {
    this.reportRepository = reportRepository;
    this.steps =
        steps.stream().sorted(Comparator.comparingInt(ReportPipelineStep::getOrder)).toList();
  }

  @Override
  @Transactional
  public void run(Long reportId) {
    log.info("Starting report pipeline for report id: {}", reportId);
    Report report =
        reportRepository
            .findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
    try {
      for (ReportPipelineStep step : steps) {
        log.debug("Executing pipeline step: {}", step.getClass().getSimpleName());
        report = step.execute(report);
      }
    } catch (Exception e) {
      log.error("Report pipeline failed for report id: {}", reportId, e);
      report.setStatus(ReportStatus.FAILED);
      reportRepository.save(report);
      return;
    }
    log.info("Completed report pipeline for report id: {}", reportId);
  }
}
