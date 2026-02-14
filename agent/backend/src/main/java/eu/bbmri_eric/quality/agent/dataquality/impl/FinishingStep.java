package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.event.ReportGeneratedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class FinishingStep implements ReportPipelineStep {

  private final EventPublisher eventPublisher;

  FinishingStep(EventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Report execute(Report report) {
    log.info("Finishing report generation for report id: {}", report.getId());
    report.setStatus(ReportStatus.GENERATED);
    eventPublisher.publishEvent(new ReportGeneratedEvent(report.getId()));
    log.info("Report id: {} marked as GENERATED", report.getId());
    return report;
  }

  @Override
  public int getOrder() {
    return 40;
  }
}
