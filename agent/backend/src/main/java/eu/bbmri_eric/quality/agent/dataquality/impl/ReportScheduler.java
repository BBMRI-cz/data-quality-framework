package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ReportScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

  private final ReportService reportService;

  ReportScheduler(ReportService reportService) {
    this.reportService = reportService;
  }

  /** Scheduled task to generate reports daily at midnight. */
  @Scheduled(cron = "0 0 0 * * *")
  void generateReport() {
    log.info("Scheduled report generation triggered");
    reportService.generateReport();
  }
}
