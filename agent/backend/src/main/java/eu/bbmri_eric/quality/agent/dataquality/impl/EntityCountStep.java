package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class EntityCountStep implements ReportPipelineStep {

  private final DataStoreFactory dataStoreFactory;

  EntityCountStep(DataStoreFactory dataStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
  }

  @Override
  public Report execute(Report report) {
    log.info("Counting entities for report id: {}", report.getId());

    DataStore dataStore = dataStoreFactory.resolveDataStore();

    Integer patientCount = dataStore.countPatients();
    Integer sampleCount = dataStore.countSecondaryEntities();

    report.setNumberOfEntities(patientCount);
    report.setNumberOfSecondaryEntities(sampleCount);

    log.info(
        "Completed entity count for report id: {} - patients: {}, secondary entities: {}",
        report.getId(),
        patientCount,
        sampleCount);
    return report;
  }

  @Override
  public int getOrder() {
    return 30;
  }
}
