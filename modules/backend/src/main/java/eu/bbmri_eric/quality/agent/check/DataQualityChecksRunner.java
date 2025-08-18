package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.events.DataQualityCheckResult;
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
class DataQualityChecksRunner {

  private static final Logger log = LoggerFactory.getLogger(DataQualityChecksRunner.class);
  private final CQLCheckRepository repository;
  private final ApplicationEventPublisher eventPublisher;
  private final FHIRStore fhirStore;

  DataQualityChecksRunner(
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
      if (dataQualityCheck instanceof StratifiedDataQualityCheck) {
        Map<String, Result> results =
            ((StratifiedDataQualityCheck) dataQualityCheck).executeWithStratification(fhirStore);
        int count = results.size();
        for (Map.Entry<String, Result> result : results.entrySet()) {
          String stratum = result.getKey();
          eventPublisher.publishEvent(
              new DataQualityCheckResult(
                  this,
                  dataQualityCheck.getId(),
                  dataQualityCheck.getName() + " (%s)".formatted(result.getKey()),
                  result.getValue().numberOfEntities(),
                  result.getValue().idSet(),
                  result.getValue().error(),
                  LocalDateTime.now(),
                  dataQualityCheck.getWarningThreshold(),
                  dataQualityCheck.getErrorThreshold(),
                  dataQualityCheck.getEpsilonBudget() / count,
                  stratum));
        }
      } else {
        Result result = dataQualityCheck.execute(fhirStore);
        eventPublisher.publishEvent(
            new DataQualityCheckResult(
                this,
                dataQualityCheck.getId(),
                dataQualityCheck.getName(),
                result.numberOfEntities(),
                result.idSet(),
                result.error(),
                LocalDateTime.now(),
                dataQualityCheck.getWarningThreshold(),
                dataQualityCheck.getErrorThreshold(),
                dataQualityCheck.getEpsilonBudget(),
                null));
      }
    }
    eventPublisher.publishEvent(new FinishedReportEvent(this, event.getReportId()));
  }
}
