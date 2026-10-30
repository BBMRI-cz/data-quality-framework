package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Counts the entities in the data store and computes their noisy counterparts. To prevent averaging
 * attacks across repeated reports, a total that stayed within {@link
 * DifferentialPrivacyUtil#NOISE_REFRESH_THRESHOLD} of the value its current noise was drawn for
 * reuses the previous noisy value instead of sampling fresh noise.
 *
 * <p>Totals always use Laplace noise (pure ε-DP), regardless of the configured noise mechanism, and
 * use different sensitivities: {@link #PATIENT_SENSITIVITY} for the patient total and {@link
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
    List<TotalsHistoryRow> history = reportRepository.findTotalsHistory(ReportStatus.GENERATED);
    report.setObfuscatedNumberOfEntities(
        obfuscateTotal(
            report.getNumberOfEntities(),
            previousTotal(
                history,
                TotalsHistoryRow::numberOfEntities,
                TotalsHistoryRow::obfuscatedNumberOfEntities,
                PATIENT_SENSITIVITY),
            settings,
            epsilonPerTotal,
            PATIENT_SENSITIVITY));
    report.setObfuscatedNumberOfSecondaryEntities(
        obfuscateTotal(
            report.getNumberOfSecondaryEntities(),
            previousTotal(
                history,
                TotalsHistoryRow::numberOfSecondaryEntities,
                TotalsHistoryRow::obfuscatedNumberOfSecondaryEntities,
                SECONDARY_ENTITY_SENSITIVITY),
            settings,
            epsilonPerTotal,
            SECONDARY_ENTITY_SENSITIVITY));
  }

  /**
   * Reconstructs the previous noisy total and its anchor for one total of the totals history. See
   * {@link NoiseAnchors} for how the anchor is reconstructed exactly.
   */
  private static NoiseAnchors.PreviousNoise<Integer> previousTotal(
      List<TotalsHistoryRow> totalsHistory,
      Function<TotalsHistoryRow, Integer> rawGetter,
      Function<TotalsHistoryRow, Integer> noisyGetter,
      int sensitivity) {
    List<NoiseAnchors.HistoryEntry<Integer>> series = new ArrayList<>();
    for (TotalsHistoryRow row : totalsHistory) {
      series.add(
          new NoiseAnchors.HistoryEntry<>(rawGetter.apply(row), noisyGetter.apply(row), null));
    }
    return NoiseAnchors.reconstruct(series, refreshThreshold(sensitivity));
  }

  /**
   * Minimum drift from the anchor that triggers a fresh draw for a total of the given sensitivity.
   * The same threshold drives both the replay in {@link NoiseAnchors} and the reuse decision here,
   * so scaling a total's threshold (e.g. the sample total by {@link #SECONDARY_ENTITY_SENSITIVITY})
   * is a one-word change in this method.
   */
  private static int refreshThreshold(int sensitivity) {
    return DifferentialPrivacyUtil.NOISE_REFRESH_THRESHOLD;
  }

  /**
   * Resolves the noisy total, reusing the previous noisy value while the raw count stays within
   * {@link #refreshThreshold} of the anchor the noise was drawn for — and it does not republish a
   * non-zero value below the current minimum suppression threshold.
   */
  private Integer obfuscateTotal(
      Integer count,
      NoiseAnchors.PreviousNoise<Integer> previous,
      SettingsDTO settings,
      double epsilon,
      int sensitivity) {
    if (count == null) {
      return null;
    }
    if (previous != null
        && Math.abs(count - previous.anchor()) < refreshThreshold(sensitivity)
        && (previous.published() == 0 || previous.published() >= settings.getMinThreshold())) {
      return previous.published();
    }
    return addNoise(count, settings, epsilon, sensitivity);
  }

  private Integer addNoise(Integer count, SettingsDTO settings, double epsilon, int sensitivity) {
    if (count == null) {
      return null;
    }
    // Totals always use Laplace (pure ε-DP): in Gaussian mode the small per-total ε would make the
    // standard deviation ≈6× larger (e.g. ≈1 200 samples at δ = 1e-8) and ruin utility.
    double noisyCount =
        DifferentialPrivacyUtil.addLaplaceNoise(
            count, epsilon, sensitivity, settings.getMinThreshold());
    return (int) Math.round(noisyCount);
  }

  @Override
  public int getOrder() {
    return 30;
  }
}
