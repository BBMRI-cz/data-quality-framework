package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.dataquality.FHIRStore;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import eu.bbmri_eric.quality.agent.dataquality.event.DQCheckResultsGatheredEvent;
import eu.bbmri_eric.quality.agent.dataquality.event.DataQualityCheckResultEvent;
import eu.bbmri_eric.quality.agent.dataquality.event.NewReportEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class DataQualityChecksRunner {

  private static final Logger log = LoggerFactory.getLogger(DataQualityChecksRunner.class);
  private final CQLCheckRepository repository;
  private final EventPublisher eventPublisher;
  private final FHIRStore fhirStore;

  DataQualityChecksRunner(
      CQLCheckRepository repository, EventPublisher eventPublisher, FHIRStore fhirStore) {
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
        Map<String, ResultDTO> results =
            ((StratifiedDataQualityCheck) dataQualityCheck).executeWithStratification(fhirStore);
        int count = results.size();
        for (Map.Entry<String, ResultDTO> result : results.entrySet()) {
          String stratum = result.getKey();
          eventPublisher.publishEvent(
              new DataQualityCheckResultEvent(
                  dataQualityCheck.getId(),
                  dataQualityCheck.getName() + " (%s)".formatted(result.getKey()),
                  result.getValue().rawResult(),
                  result.getValue().idSet(),
                  result.getValue().error(),
                  LocalDateTime.now(),
                  dataQualityCheck.getWarningThreshold(),
                  dataQualityCheck.getErrorThreshold(),
                  dataQualityCheck.getEpsilonBudget() / count,
                  stratum));
        }
      } else {
        ResultDTO result = dataQualityCheck.execute(fhirStore);
        eventPublisher.publishEvent(
            new DataQualityCheckResultEvent(
                dataQualityCheck.getId(),
                dataQualityCheck.getName(),
                result.rawResult(),
                result.idSet(),
                result.error(),
                LocalDateTime.now(),
                dataQualityCheck.getWarningThreshold(),
                dataQualityCheck.getErrorThreshold(),
                dataQualityCheck.getEpsilonBudget(),
                null));
      }
    }
    eventPublisher.publishEvent(new DQCheckResultsGatheredEvent(event.getReportId()));
  }
}
