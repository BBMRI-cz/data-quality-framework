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
import java.util.List;
import java.util.function.Function;
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
    List<Report> history =
        reportRepository.findAllByStatusIsOrderByGeneratedAtDesc(ReportStatus.GENERATED);
    Integer patientsAnchor =
        resolveAnchor(history, Report::getNumberOfEntities, Report::getObfuscatedNumberOfEntities);
    report.setObfuscatedNumberOfEntities(
        obfuscateTotal(
            report.getNumberOfEntities(),
            patientsAnchor,
            previousNoisy(history, Report::getObfuscatedNumberOfEntities),
            settings,
            epsilonPerTotal));
    Integer secondaryAnchor =
        resolveAnchor(
            history,
            Report::getNumberOfSecondaryEntities,
            Report::getObfuscatedNumberOfSecondaryEntities);
    report.setObfuscatedNumberOfSecondaryEntities(
        obfuscateTotal(
            report.getNumberOfSecondaryEntities(),
            secondaryAnchor,
            previousNoisy(history, Report::getObfuscatedNumberOfSecondaryEntities),
            settings,
            epsilonPerTotal));
  }

  private static Integer previousNoisy(
      List<Report> historyNewestFirst, Function<Report, Integer> noisyGetter) {
    return historyNewestFirst.isEmpty() ? null : noisyGetter.apply(historyNewestFirst.getFirst());
  }

  /**
   * Derives the anchor — the raw total the latest noisy total was drawn for — from the report
   * history: reused noise propagates bit-for-bit, so the anchor is the raw total of the oldest
   * report in the contiguous backwards run of reports sharing the current noisy value. Returns
   * {@code null} when there is no usable previous noisy value (and hence no reuse).
   */
  private static Integer resolveAnchor(
      List<Report> historyNewestFirst,
      Function<Report, Integer> rawGetter,
      Function<Report, Integer> noisyGetter) {
    if (historyNewestFirst.isEmpty()) {
      return null;
    }
    Integer chainValue = noisyGetter.apply(historyNewestFirst.getFirst());
    if (chainValue == null) {
      return null;
    }
    Integer anchor = rawGetter.apply(historyNewestFirst.getFirst());
    for (Report pastReport : historyNewestFirst) {
      if (!chainValue.equals(noisyGetter.apply(pastReport))) {
        break;
      }
      Integer raw = rawGetter.apply(pastReport);
      if (raw != null) {
        anchor = raw;
      }
    }
    return anchor;
  }

  /**
   * Resolves the noisy total, reusing the previous noisy value while the raw count stays within
   * {@link DifferentialPrivacyUtil#NOISE_REFRESH_THRESHOLD} of the anchor the noise was drawn for.
   */
  private Integer obfuscateTotal(
      Integer count,
      Integer previousAnchor,
      Integer previousNoisyCount,
      SettingsDTO settings,
      double epsilon) {
    if (count == null) {
      return null;
    }
    if (previousNoisyCount != null
        && previousAnchor != null
        && Math.abs(count - previousAnchor) < DifferentialPrivacyUtil.NOISE_REFRESH_THRESHOLD) {
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
