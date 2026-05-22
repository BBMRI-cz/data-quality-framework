package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class EntityCountStep implements ReportPipelineStep {

  private final FHIRServer fhirStore;

  EntityCountStep(FHIRServer fhirStore) {
    this.fhirStore = fhirStore;
  }

  @Override
  public Report execute(Report report) {
    log.info("Counting entities for report id: {}", report.getId());

    Integer patientCount = fhirStore.countResources("Patient");
    Integer sampleCount = fhirStore.countResources("Specimen");

    report.setNumberOfEntities(patientCount);
    report.setNumberOfSecondaryEntities(sampleCount);

    log.info(
        "Completed entity count for report id: {} - patients: {}, specimens: {}",
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
