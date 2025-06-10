package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.events.CheckResultEvent;
import eu.bbmri_eric.quality.agent.events.FinishedReportEvent;
import eu.bbmri_eric.quality.agent.events.NewReportEvent;
import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class QualityCheckRunner {

  private static final Logger log = LoggerFactory.getLogger(QualityCheckRunner.class);
  private final CQLCheckRepository repository;
  private final ApplicationEventPublisher eventPublisher;
  private final FHIRStore fhirStore;

  QualityCheckRunner(
      CQLCheckRepository repository,
      ApplicationEventPublisher eventPublisher,
      FHIRStore fhirStore) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.fhirStore = fhirStore;
  }

  @EventListener
  void onNewReport(NewReportEvent event) {
    log.info("New report received: {} | Running Quality Checks...", event.getReportId());
    List<CQLCheck> checks = repository.findAll();
    for (CQLCheck check : checks) {
      Result result = check.execute(fhirStore);
      eventPublisher.publishEvent(
          new CheckResultEvent(
              this,
              check.getId(),
              check.getName(),
              result.numberOfEntities(),
              null,
              LocalDateTime.now(),
              check.getWarningThreshold(),
              check.getErrorThreshold()));
    }
    eventPublisher.publishEvent(new FinishedReportEvent(this, event.getReportId()));
  }
}
