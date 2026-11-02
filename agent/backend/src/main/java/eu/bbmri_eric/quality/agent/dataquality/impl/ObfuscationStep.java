package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.privacy.CountReleaser;
import eu.bbmri_eric.quality.agent.dataquality.privacy.EpsilonAllocator;
import eu.bbmri_eric.quality.agent.dataquality.privacy.HistoryEntry;
import eu.bbmri_eric.quality.agent.dataquality.privacy.LaplaceSampler;
import eu.bbmri_eric.quality.agent.dataquality.privacy.NoiseHistory;
import eu.bbmri_eric.quality.agent.dataquality.privacy.NoiseSampler;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PreviousRelease;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PrivacyBudget;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PrivacyParameters;
import eu.bbmri_eric.quality.agent.dataquality.privacy.ResultHistoryRow;
import eu.bbmri_eric.quality.agent.dataquality.privacy.TotalsHistoryRow;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This pipeline step takes care of adding noise to a report's metrics: the individual check results
 * and the entity totals.
 *
 * <p>To prevent averaging attacks across repeated reports, a metric whose raw count stayed within
 * the refresh threshold of its anchor reuses the previously published noisy value instead of
 * drawing fresh noise. The reuse rule lives in {@link CountReleaser}; the anchor is reconstructed
 * from the publication history by {@link NoiseHistory}.
 *
 * <p>Check results use the configured noise mechanism with sensitivity {@link #RESULT_SENSITIVITY}.
 * Entity totals always use Laplace noise (pure ε-DP) and their own per-individual sensitivities,
 * since one patient can contribute many samples.
 */
@Slf4j
@Component
class ObfuscationStep implements ReportPipelineStep {

  private static final int RESULT_SENSITIVITY = 1;
  private static final int PATIENT_SENSITIVITY = 1;

  /**
   * Assumed maximum number of samples (secondary entities) per patient. Used as the sensitivity of
   * the noisy sample total; patients with more samples are protected proportionally less for this
   * number. Documented in docs/user/privacy.md.
   */
  private static final int SECONDARY_ENTITY_SENSITIVITY = 20;

  private final SettingsService settingsService;
  private final QualityCheckService qualityCheckService;
  private final ReportRepository reportRepository;
  private final RandomGenerator randomGenerator;

  ObfuscationStep(
      SettingsService settingsService,
      QualityCheckService qualityCheckService,
      ReportRepository reportRepository,
      RandomGenerator randomGenerator) {
    this.settingsService = settingsService;
    this.qualityCheckService = qualityCheckService;
    this.reportRepository = reportRepository;
    this.randomGenerator = randomGenerator;
  }

  @Override
  public Report execute(Report report) {
    SettingsDTO settings = settingsService.getSettings();
    PrivacyParameters parameters = PrivacyParameters.from(settings);
    obfuscateResults(report, parameters);
    obfuscateEntityTotals(report, parameters);
    log.info("Completed obfuscation for report id: {}", report.getId());
    return report;
  }

  @Override
  public int getOrder() {
    return OBFUSCATION_ORDER;
  }

  private void obfuscateResults(Report report, PrivacyParameters parameters) {
    List<Result> results = resultsWithNonNullRawValues(report.getResults());
    if (results.isEmpty()) {
      return;
    }
    log.debug("Adding obfuscated values for report id: {}", report.getId());
    CountReleaser releaser =
        new CountReleaser(
            NoiseSampler.of(parameters.mechanism(), randomGenerator, parameters.delta()),
            parameters.minThreshold());
    List<Double> epsilons = allocateBudget(results, parameters);
    Map<CheckInstanceKey, PreviousRelease> previousReleases =
        previousReleasesByCheckAndStratum(results);
    for (int i = 0; i < results.size(); i++) {
      Result result = results.get(i);
      PreviousRelease previous = previousReleases.get(CheckInstanceKey.of(result));
      CountReleaser.Release release =
          releaser.release(result.getRawValue(), previous, epsilons.get(i), RESULT_SENSITIVITY);
      result.setObfuscatedValue(release.value());
      result.setEpsilon(release.epsilon());
    }
  }

  private void obfuscateEntityTotals(Report report, PrivacyParameters parameters) {
    // Totals always use Laplace (pure ε-DP): at the small per-total ε the Gaussian standard
    // deviation would be several times larger than the Laplace scale and ruin utility.
    CountReleaser releaser =
        new CountReleaser(new LaplaceSampler(randomGenerator), parameters.minThreshold());
    double epsilonPerTotal = PrivacyBudget.epsilonPerEntityTotal(parameters.epsilon());
    List<TotalsHistoryRow> history = reportRepository.findTotalsHistory(ReportStatus.GENERATED);
    report.setObfuscatedNumberOfEntities(
        releaseTotal(
            report.getNumberOfEntities(),
            history,
            TotalsHistoryRow::numberOfEntities,
            TotalsHistoryRow::obfuscatedNumberOfEntities,
            releaser,
            epsilonPerTotal,
            PATIENT_SENSITIVITY));
    report.setObfuscatedNumberOfSecondaryEntities(
        releaseTotal(
            report.getNumberOfSecondaryEntities(),
            history,
            TotalsHistoryRow::numberOfSecondaryEntities,
            TotalsHistoryRow::obfuscatedNumberOfSecondaryEntities,
            releaser,
            epsilonPerTotal,
            SECONDARY_ENTITY_SENSITIVITY));
  }

  /** Releases one total: replays its history to the previous release, then releases or reuses. */
  private Integer releaseTotal(
      Integer count,
      List<TotalsHistoryRow> history,
      Function<TotalsHistoryRow, Integer> rawGetter,
      Function<TotalsHistoryRow, Integer> noisyGetter,
      CountReleaser releaser,
      double epsilon,
      int sensitivity) {
    if (count == null) {
      return null;
    }
    PreviousRelease previous =
        previousRelease(history, rawGetter, noisyGetter, sensitivity).orElse(null);
    return (int) Math.round(releaser.release(count, previous, epsilon, sensitivity).value());
  }

  /** Reconstructs the previous release for one total column pair of the totals history. */
  private static java.util.Optional<PreviousRelease> previousRelease(
      List<TotalsHistoryRow> history,
      Function<TotalsHistoryRow, Integer> rawGetter,
      Function<TotalsHistoryRow, Integer> noisyGetter,
      int sensitivity) {
    List<HistoryEntry> series = new ArrayList<>();
    for (TotalsHistoryRow row : history) {
      Integer published = noisyGetter.apply(row);
      series.add(
          new HistoryEntry(
              rawGetter.apply(row), published != null ? published.doubleValue() : null, null));
    }
    return NoiseHistory.replay(series, PrivacyBudget.refreshThreshold(sensitivity));
  }

  private static List<Result> resultsWithNonNullRawValues(List<Result> results) {
    return results.stream().filter(r -> r.getRawValue() != null).toList();
  }

  private List<Double> allocateBudget(List<Result> results, PrivacyParameters parameters) {
    return EpsilonAllocator.allocate(
        PrivacyBudget.epsilonForCheckResults(parameters.epsilon()),
        results.stream().map(Result::getCheckId).toList(),
        preferredBudgetByCheckId());
  }

  private Map<Long, Double> preferredBudgetByCheckId() {
    return qualityCheckService.findAll().stream()
        .filter(qc -> qc.getEpsilonBudget() != null)
        .collect(Collectors.toMap(QualityCheckDTO::getId, QualityCheckDTO::getEpsilonBudget));
  }

  /**
   * Reconstructs, for every check instance in the given results, the previously published noisy
   * value and its anchor from one projection query over the report history.
   */
  private Map<CheckInstanceKey, PreviousRelease> previousReleasesByCheckAndStratum(
      List<Result> currentResults) {
    java.util.Set<CheckInstanceKey> wanted =
        currentResults.stream().map(CheckInstanceKey::of).collect(Collectors.toSet());
    Map<CheckInstanceKey, List<HistoryEntry>> seriesByKey = new HashMap<>();
    for (ResultHistoryRow row : reportRepository.findResultHistory(ReportStatus.GENERATED)) {
      CheckInstanceKey key = new CheckInstanceKey(row.checkId(), row.stratum());
      if (!wanted.contains(key)) {
        continue;
      }
      seriesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(row.toHistoryEntry());
    }
    Map<CheckInstanceKey, PreviousRelease> previousReleases = new HashMap<>();
    seriesByKey.forEach(
        (key, series) ->
            NoiseHistory.replay(series, PrivacyBudget.refreshThreshold(RESULT_SENSITIVITY))
                .ifPresent(previous -> previousReleases.put(key, previous)));
    return previousReleases;
  }

  private record CheckInstanceKey(Long checkId, String stratum) {
    private static CheckInstanceKey of(Result result) {
      return new CheckInstanceKey(result.getCheckId(), result.getStratum());
    }
  }
}
