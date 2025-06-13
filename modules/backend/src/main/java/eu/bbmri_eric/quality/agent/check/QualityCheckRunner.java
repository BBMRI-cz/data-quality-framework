package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.events.CheckResultEvent;
import eu.bbmri_eric.quality.agent.events.FinishedReportEvent;
import eu.bbmri_eric.quality.agent.events.NewReportEvent;
import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    List<Check> checks =
        new ArrayList<>(repository.findAll().stream().map(Check.class::cast).toList());
    checks.add(new DuplicateIdentifierCheck());
    checks.add(new SurvivalRateCheck());
    for (Check check : checks) {
      if (check instanceof CheckWithStratification) {
        Map<String, Result> results =
            ((CheckWithStratification) check).executeWithStratification(fhirStore);
        int count = results.size();
        for (Map.Entry<String, Result> result : results.entrySet()) {
          eventPublisher.publishEvent(
              new CheckResultEvent(
                  this,
                  check.getId(),
                  check.getName() + " (%s)".formatted(result.getKey()),
                  result.getValue().numberOfEntities(),
                  result.getValue().error(),
                  LocalDateTime.now(),
                  check.getWarningThreshold(),
                  check.getErrorThreshold(),
                  check.getEpsilonBudget() / count));
        }
      } else {
        Result result = check.execute(fhirStore);
        eventPublisher.publishEvent(
            new CheckResultEvent(
                this,
                check.getId(),
                check.getName(),
                result.numberOfEntities(),
                result.error(),
                LocalDateTime.now(),
                check.getWarningThreshold(),
                check.getErrorThreshold(),
                check.getEpsilonBudget()));
      }
    }
    eventPublisher.publishEvent(new FinishedReportEvent(this, event.getReportId()));
  }
}
