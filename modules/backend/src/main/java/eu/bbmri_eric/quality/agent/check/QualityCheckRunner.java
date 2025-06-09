package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.report.NewReportEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class QualityCheckRunner {

  private static final Logger log = LoggerFactory.getLogger(QualityCheckRunner.class);
  private final CQLCheckRepository repository;

  QualityCheckRunner(CQLCheckRepository repository) {
    this.repository = repository;
  }

  @EventListener
  void onNewReport(NewReportEvent event) {
    log.info("New report received: {} | Running Quality Checks...", event.getReportId());
    List<CQLCheck> checks = repository.findAll();
    for (CQLCheck check : checks) {
      log.info(check.execute().toString());
    }
  }
}
