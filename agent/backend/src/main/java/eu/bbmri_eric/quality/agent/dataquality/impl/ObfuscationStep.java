package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.privacy.CountReleaser;
import eu.bbmri_eric.quality.agent.dataquality.privacy.EpsilonAllocator;
import eu.bbmri_eric.quality.agent.dataquality.privacy.NoiseHistory;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PrivacyBudget;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PrivacyParameters;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This pipeline step takes care of adding noise to individual report results.
 *
 * <p>To prevent averaging attacks across repeated reports, a result whose raw count stayed within
 * the refresh threshold of its anchor reuses the previously published noisy value instead of
 * drawing fresh noise. The reuse rule lives in {@link CountReleaser}; the anchor is reconstructed
 * from the publication history by {@link NoiseHistory}.
 */
@Slf4j
@Component
class ObfuscationStep implements ReportPipelineStep {

  private static final int RESULT_SENSITIVITY = 1;

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
    obfuscateResults(report);
    log.info("Completed obfuscation for report id: {}", report.getId());
    return report;
  }

  private void obfuscateResults(Report report) {
    log.info("Adding obfuscated values for report id: {}", report.getId());
    List<Result> results = resultsWithNonNullRawValues(report.getResults());
    if (results.isEmpty()) {
      return;
    }
    SettingsDTO settings = settingsService.getSettings();
    PrivacyParameters parameters = PrivacyParameters.from(settings);
    CountReleaser releaser = CountReleaser.forParameters(parameters, randomGenerator);
    double[] epsilons = allocateBudget(results, parameters);
    Map<CheckInstanceKey, NoiseHistory.PreviousRelease> previousReleases =
        previousReleasesByCheckAndStratum(results);
    for (int i = 0; i < results.size(); i++) {
      Result result = results.get(i);
      NoiseHistory.PreviousRelease previous = previousReleases.get(CheckInstanceKey.of(result));
      CountReleaser.Release release =
          releaser.release(result.getRawValue(), previous, epsilons[i], RESULT_SENSITIVITY);
      result.setObfuscatedValue(release.value());
      result.setEpsilon(release.reused() ? previous.epsilon() : epsilons[i]);
    }
  }

  private static List<Result> resultsWithNonNullRawValues(List<Result> results) {
    return results.stream().filter(r -> r.getRawValue() != null).toList();
  }

  private double[] allocateBudget(List<Result> results, PrivacyParameters parameters) {
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
  private Map<CheckInstanceKey, NoiseHistory.PreviousRelease> previousReleasesByCheckAndStratum(
      List<Result> currentResults) {
    Set<CheckInstanceKey> wanted =
        currentResults.stream().map(CheckInstanceKey::of).collect(Collectors.toSet());
    Map<CheckInstanceKey, List<NoiseHistory.HistoryEntry>> seriesByKey = new HashMap<>();
    for (ResultHistoryRow row : reportRepository.findResultHistory(ReportStatus.GENERATED)) {
      CheckInstanceKey key = new CheckInstanceKey(row.checkId(), row.stratum());
      if (!wanted.contains(key)) continue;
      seriesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(row.toHistoryEntry());
    }
    Map<CheckInstanceKey, NoiseHistory.PreviousRelease> previousReleases = new HashMap<>();
    seriesByKey.forEach(
        (key, series) -> {
          NoiseHistory.PreviousRelease previous =
              NoiseHistory.replay(series, PrivacyBudget.refreshThreshold(RESULT_SENSITIVITY));
          if (previous != null) {
            previousReleases.put(key, previous);
          }
        });
    return previousReleases;
  }

  private record CheckInstanceKey(Long checkId, String stratum) {
    private static CheckInstanceKey of(Result result) {
      return new CheckInstanceKey(result.getCheckId(), result.getStratum());
    }
  }

  @Override
  public int getOrder() {
    return 20;
  }
}
