package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.privacy.CountReleaser;
import eu.bbmri_eric.quality.agent.dataquality.privacy.Mechanism;
import eu.bbmri_eric.quality.agent.dataquality.privacy.NoiseHistory;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PrivacyBudget;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PrivacyParameters;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Counts the entities in the data store and computes their noisy counterparts. To prevent averaging
 * attacks across repeated reports, a total that stayed within the refresh threshold of its anchor
 * reuses the previous noisy value (same rule as for check results, shared via {@link
 * CountReleaser}).
 *
 * <p>Totals always use Laplace noise (pure ε-DP), regardless of the configured noise mechanism —
 * the Gaussian σ at the small per-total budgets would be far too large to be useful. Sensitivities
 * differ per individual: {@link #PATIENT_SENSITIVITY} for the patient total and {@link
 * #SECONDARY_ENTITY_SENSITIVITY} for the sample total, since one patient can contribute many
 * samples.
 */
@Slf4j
@Component
class EntityCountStep implements ReportPipelineStep {

  private static final int PATIENT_SENSITIVITY = 1;

  /**
   * Assumed maximum number of samples (secondary entities) per patient. Used as the sensitivity of
   * the noisy sample total; patients with more samples are protected proportionally less for this
   * number. Documented in docs/user/privacy.md.
   */
  static final int SECONDARY_ENTITY_SENSITIVITY = 20;

  private final DataStoreFactory dataStoreFactory;
  private final SettingsService settingsService;
  private final ReportRepository reportRepository;
  private final RandomGenerator randomGenerator;

  EntityCountStep(
      DataStoreFactory dataStoreFactory,
      SettingsService settingsService,
      ReportRepository reportRepository,
      RandomGenerator randomGenerator) {
    this.dataStoreFactory = dataStoreFactory;
    this.settingsService = settingsService;
    this.reportRepository = reportRepository;
    this.randomGenerator = randomGenerator;
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
    PrivacyParameters parameters = PrivacyParameters.from(settingsService.getSettings());
    // Totals always use Laplace (pure ε-DP): in Gaussian mode the small per-total ε would make
    // the standard deviation ≈6× larger (e.g. ≈1 200 samples at δ = 1e-8) and ruin utility.
    CountReleaser releaser =
        new CountReleaser(parameters, Mechanism.of(NoiseMechanism.LAPLACE, randomGenerator));
    double epsilonPerTotal = PrivacyBudget.epsilonPerEntityTotal(parameters.epsilon());
    List<TotalsHistoryRow> history = reportRepository.findTotalsHistory(ReportStatus.GENERATED);
    report.setObfuscatedNumberOfEntities(
        releaseTotal(
            report.getNumberOfEntities(),
            previousTotal(
                history,
                TotalsHistoryRow::numberOfEntities,
                TotalsHistoryRow::obfuscatedNumberOfEntities,
                PATIENT_SENSITIVITY),
            releaser,
            epsilonPerTotal,
            PATIENT_SENSITIVITY));
    report.setObfuscatedNumberOfSecondaryEntities(
        releaseTotal(
            report.getNumberOfSecondaryEntities(),
            previousTotal(
                history,
                TotalsHistoryRow::numberOfSecondaryEntities,
                TotalsHistoryRow::obfuscatedNumberOfSecondaryEntities,
                SECONDARY_ENTITY_SENSITIVITY),
            releaser,
            epsilonPerTotal,
            SECONDARY_ENTITY_SENSITIVITY));
  }

  private Integer releaseTotal(
      Integer count,
      NoiseHistory.PreviousRelease previous,
      CountReleaser releaser,
      double epsilon,
      int sensitivity) {
    if (count == null) {
      return null;
    }
    return (int) Math.round(releaser.release(count, previous, epsilon, sensitivity).value());
  }

  /** Reconstructs the previous release and anchor for one total column pair of the history. */
  private static NoiseHistory.PreviousRelease previousTotal(
      List<TotalsHistoryRow> totalsHistory,
      Function<TotalsHistoryRow, Integer> rawGetter,
      Function<TotalsHistoryRow, Integer> noisyGetter,
      int sensitivity) {
    List<NoiseHistory.HistoryEntry> series = new ArrayList<>();
    for (TotalsHistoryRow row : totalsHistory) {
      Integer published = noisyGetter.apply(row);
      series.add(
          new NoiseHistory.HistoryEntry(
              rawGetter.apply(row), published == null ? Double.NaN : published, null));
    }
    return NoiseHistory.replay(series, PrivacyBudget.refreshThreshold(sensitivity));
  }

  @Override
  public int getOrder() {
    return 30;
  }
}
