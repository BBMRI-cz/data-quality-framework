package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Counts the entities in the data store and computes their noisy counterparts. To prevent averaging
 * attacks across repeated reports, a total whose raw count is unchanged compared to the previous
 * generated report reuses the previous noisy value instead of sampling fresh noise.
 */
@Slf4j
@Component
class EntityCountStep implements ReportPipelineStep {

  private final DataStoreFactory dataStoreFactory;
  private final SettingsService settingsService;
  private final ReportRepository reportRepository;

  EntityCountStep(
      DataStoreFactory dataStoreFactory,
      SettingsService settingsService,
      ReportRepository reportRepository) {
    this.dataStoreFactory = dataStoreFactory;
    this.settingsService = settingsService;
    this.reportRepository = reportRepository;
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
    Optional<Report> previous =
        reportRepository.findTopByStatusIsOrderByGeneratedAtDesc(ReportStatus.GENERATED);
    report.setObfuscatedNumberOfEntities(
        resolveTotal(
            report.getNumberOfEntities(),
            previous.map(Report::getNumberOfEntities).orElse(null),
            previous.map(Report::getObfuscatedNumberOfEntities).orElse(null),
            settings,
            epsilonPerTotal));
    report.setObfuscatedNumberOfSecondaryEntities(
        resolveTotal(
            report.getNumberOfSecondaryEntities(),
            previous.map(Report::getNumberOfSecondaryEntities).orElse(null),
            previous.map(Report::getObfuscatedNumberOfSecondaryEntities).orElse(null),
            settings,
            epsilonPerTotal));
  }

  private Integer resolveTotal(
      Integer count,
      Integer previousCount,
      Integer previousNoisyCount,
      SettingsDTO settings,
      double epsilon) {
    if (count == null) {
      return null;
    }
    if (previousNoisyCount != null && Objects.equals(count, previousCount)) {
      return previousNoisyCount;
    }
    return addNoise(count, settings, epsilon);
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
