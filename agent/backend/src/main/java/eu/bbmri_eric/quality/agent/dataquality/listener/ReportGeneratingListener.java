package eu.bbmri_eric.quality.agent.dataquality.listener;

import eu.bbmri_eric.quality.agent.dataquality.ReportPipeline;
import eu.bbmri_eric.quality.agent.dataquality.event.NewReportEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
class ReportGeneratingListener {

  private final ReportPipeline reportPipeline;

  ReportGeneratingListener(ReportPipeline reportPipeline) {
    this.reportPipeline = reportPipeline;
  }

  @Async
  @TransactionalEventListener
  public void onNewReportEvent(NewReportEvent event) {
    log.info("Received new report event for report id: {}", event.getReportId());
    reportPipeline.run(event.getReportId());
  }
}
