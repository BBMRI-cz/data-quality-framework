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
    List<DataQualityCheck> dataQualityChecks =
        new ArrayList<>(repository.findAll().stream().map(DataQualityCheck.class::cast).toList());
    dataQualityChecks.add(new DuplicateIdentifierCheck());
    dataQualityChecks.add(new SurvivalRateCheck());
    dataQualityChecks.add(new InvalidConditionICDCheck());
    for (DataQualityCheck dataQualityCheck : dataQualityChecks) {
      if (dataQualityCheck instanceof CheckWithStratification) {
        Map<String, Result> results =
            ((CheckWithStratification) dataQualityCheck).executeWithStratification(fhirStore);
        int count = results.size();
        for (Map.Entry<String, Result> result : results.entrySet()) {
          eventPublisher.publishEvent(
              new CheckResultEvent(
                  this,
                  dataQualityCheck.getId(),
                  dataQualityCheck.getName() + " (%s)".formatted(result.getKey()),
                  result.getValue().numberOfEntities(),
                  result.getValue().error(),
                  LocalDateTime.now(),
                  dataQualityCheck.getWarningThreshold(),
                  dataQualityCheck.getErrorThreshold(),
                  dataQualityCheck.getEpsilonBudget() / count));
        }
      } else {
        Result result = dataQualityCheck.execute(fhirStore);
        eventPublisher.publishEvent(
            new CheckResultEvent(
                this,
                dataQualityCheck.getId(),
                dataQualityCheck.getName(),
                result.numberOfEntities(),
                result.error(),
                LocalDateTime.now(),
                dataQualityCheck.getWarningThreshold(),
                dataQualityCheck.getErrorThreshold(),
                dataQualityCheck.getEpsilonBudget()));
      }
    }
    eventPublisher.publishEvent(new FinishedReportEvent(this, event.getReportId()));
  }
}
