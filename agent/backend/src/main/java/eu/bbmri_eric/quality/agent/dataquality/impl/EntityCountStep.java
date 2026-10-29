package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Counts the entities in the data store and computes their noisy counterparts. The noisy counts are
 * computed exactly once and persisted on the report, so repeated releases of the same report cannot
 * leak information through re-sampling (averaging) of the noise.
 */
@Slf4j
@Component
class EntityCountStep implements ReportPipelineStep {

  private final DataStoreFactory dataStoreFactory;
  private final SettingsService settingsService;

  EntityCountStep(DataStoreFactory dataStoreFactory, SettingsService settingsService) {
    this.dataStoreFactory = dataStoreFactory;
    this.settingsService = settingsService;
  }

  @Override
  public Report execute(Report report) {
    log.info("Counting entities for report id: {}", report.getId());

    DataStore dataStore = dataStoreFactory.resolveDataStore();

    Integer patientCount = dataStore.countPatients();
    Integer sampleCount = dataStore.countSecondaryEntities();

    report.setNumberOfEntities(patientCount);
    report.setNumberOfSecondaryEntities(sampleCount);

    obfuscateEntityTotals(report);

    log.info(
        "Completed entity count for report id: {} - patients: {}, secondary entities: {}",
        report.getId(),
        patientCount,
        sampleCount);
    return report;
  }

  private void obfuscateEntityTotals(Report report) {
    SettingsDTO settings = settingsService.getSettings();
    double epsilonPerTotal =
        settings.getEpsilon() * DifferentialPrivacyUtil.ENTITY_TOTALS_EPSILON_FRACTION / 2;
    report.setObfuscatedNumberOfEntities(
        addNoise(report.getNumberOfEntities(), settings, epsilonPerTotal));
    report.setObfuscatedNumberOfSecondaryEntities(
        addNoise(report.getNumberOfSecondaryEntities(), settings, epsilonPerTotal));
  }

  private Integer addNoise(Integer count, SettingsDTO settings, double epsilon) {
    if (count == null) {
      return null;
    }
    double noisyCount =
        settings.getNoiseMechanism() == NoiseMechanism.GAUSSIAN
            ? DifferentialPrivacyUtil.addGaussianNoise(
                count, epsilon, settings.getDelta(), 1, settings.getMinThreshold())
            : DifferentialPrivacyUtil.addLaplaceNoise(
                count, epsilon, 1, settings.getMinThreshold());
    return (int) Math.round(noisyCount);
  }

  @Override
  public int getOrder() {
    return 30;
  }
}
