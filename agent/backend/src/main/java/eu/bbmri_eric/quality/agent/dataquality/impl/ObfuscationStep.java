package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This pipeline step takes care of adding noise to individual report results.
 *
 * <p>To prevent averaging attacks across repeated reports, a result whose raw count stayed within
 * {@link DifferentialPrivacyUtil#NOISE_REFRESH_THRESHOLD} of the value its current noise was drawn
 * for (its anchor) reuses the previous noisy value instead of sampling fresh noise. Larger drifts
 * get fresh noise and move the anchor.
 */
@Slf4j
@Component
class ObfuscationStep implements ReportPipelineStep {
  private final SettingsService settingsService;
  private final QualityCheckService qualityCheckService;
  private final ReportRepository reportRepository;

  ObfuscationStep(
      SettingsService settingsService,
      QualityCheckService qualityCheckService,
      ReportRepository reportRepository) {
    this.settingsService = settingsService;
    this.qualityCheckService = qualityCheckService;
    this.reportRepository = reportRepository;
  }

  @Override
  public Report execute(Report report) {
    obfuscateResults(report);
    log.info("Completed obfuscation for report id: {}", report.getId());
    return report;
  }

  @Override
  public int getOrder() {
    return 20;
  }

  private List<Result> resultsWithNonNullValues(List<Result> results) {
    return results.stream().filter(r -> r.getRawValue() != null).toList();
  }

  private double computeTotalPreferredEpsilon(
      List<Result> results, Map<Long, Double> preferredBudgetByCheckId) {
    return results.stream()
        .mapToDouble(r -> preferredBudgetByCheckId.getOrDefault(r.getCheckId(), 0.0))
        .sum();
  }

  private long countResultsWithoutPreference(
      List<Result> results, Map<Long, Double> preferredBudgetByCheckId) {
    return results.stream()
        .filter(r -> !preferredBudgetByCheckId.containsKey(r.getCheckId()))
        .count();
  }

  private double computeBaseEpsilon(
      double totalEpsilon,
      double totalPreferredEpsilon,
      int resultCount,
      long noPreferenceCount,
      boolean ignorePreferences) {
    if (ignorePreferences) return totalEpsilon / resultCount;
    if (noPreferenceCount > 0) {
      double remaining = totalEpsilon - totalPreferredEpsilon;
      if (remaining <= 0) {
        throw new IllegalArgumentException(
            "Per-check epsilon budget preferences (%s) consume the entire epsilon budget available"
                + " for check results (%s), leaving no epsilon for %d result(s) without a"
                + " preference. Reduce the preferred budgets or increase the overall epsilon"
                + " budget.".formatted(totalPreferredEpsilon, totalEpsilon, noPreferenceCount));
      }
      return remaining / noPreferenceCount;
    }
    return 0;
  }

  private double resolveEpsilon(
      Result result,
      Map<Long, Double> preferredBudgetByCheckId,
      double baseEpsilon,
      boolean ignorePreferences) {
    if (ignorePreferences) return baseEpsilon;
    return preferredBudgetByCheckId.getOrDefault(result.getCheckId(), baseEpsilon);
  }

  private void applyNoise(
      Result result, double epsilon, NoiseMechanism noiseMechanism, double delta, int threshold) {
    double noisyValue;
    if (noiseMechanism == NoiseMechanism.GAUSSIAN) {
      noisyValue =
          DifferentialPrivacyUtil.addGaussianNoise(
              result.getRawValue(), epsilon, delta, 1, threshold);
    } else {
      noisyValue =
          DifferentialPrivacyUtil.addLaplaceNoise(result.getRawValue(), epsilon, 1, threshold);
    }
    result.setObfuscatedValue(noisyValue);
    result.setEpsilon(epsilon);
  }

  private void obfuscateResults(Report report) {
    log.info("Adding obfuscated values for report id: {}", report.getId());
    List<Result> results = report.getResults();
    var settings = settingsService.getSettings();
    double totalEpsilon =
        settings.getEpsilon() * DifferentialPrivacyUtil.CHECK_RESULTS_EPSILON_FRACTION;
    double delta = settings.getDelta();
    NoiseMechanism noiseMechanism = settings.getNoiseMechanism();
    int threshold = settings.getMinThreshold();
    Map<Long, Double> preferredBudgetByCheckId = getPreferredBudgetByCheckId();
    List<Result> resultsToObfuscate = resultsWithNonNullValues(results);
    if (resultsToObfuscate.isEmpty()) return;
    double totalPreferredEpsilon =
        computeTotalPreferredEpsilon(resultsToObfuscate, preferredBudgetByCheckId);
    boolean ignorePreferences = totalPreferredEpsilon > totalEpsilon;
    long noPreferenceCount =
        countResultsWithoutPreference(resultsToObfuscate, preferredBudgetByCheckId);
    double baseEpsilon =
        computeBaseEpsilon(
            totalEpsilon,
            totalPreferredEpsilon,
            resultsToObfuscate.size(),
            noPreferenceCount,
            ignorePreferences);
    logAllocation(
        totalEpsilon,
        totalPreferredEpsilon,
        resultsToObfuscate.size(),
        noPreferenceCount,
        ignorePreferences);
    Map<CheckInstanceKey, NoiseAnchors.PreviousNoise<Double>> previousNoise =
        previousNoiseByCheckAndStratum(results);
    for (Result result : results) {
      if (result.getRawValue() == null) continue;
      NoiseAnchors.PreviousNoise<Double> previous = previousNoise.get(CheckInstanceKey.of(result));
      if (canReuse(previous, result, threshold)) {
        result.setObfuscatedValue(previous.published());
        result.setEpsilon(previous.epsilon());
        continue;
      }
      double epsilon =
          resolveEpsilon(result, preferredBudgetByCheckId, baseEpsilon, ignorePreferences);
      applyNoise(result, epsilon, noiseMechanism, delta, threshold);
    }
  }

  /**
   * Decides whether the previous noisy value can be reused: only when the raw count has drifted
   * less than {@link DifferentialPrivacyUtil#NOISE_REFRESH_THRESHOLD} away from the anchor (the raw
   * value the noise was drawn for), and the reused value is not republishing a non-zero count below
   * the current minimum suppression threshold. Reusing on every tiny change would reveal exactly
   * when counts move — and which checks a known patient affected.
   */
  private boolean canReuse(
      NoiseAnchors.PreviousNoise<Double> previous, Result current, int minThreshold) {
    return previous != null
        && Math.abs(current.getRawValue() - previous.anchor())
            < DifferentialPrivacyUtil.NOISE_REFRESH_THRESHOLD
        && (previous.published() == 0.0 || previous.published() >= minThreshold);
  }

  /**
   * Resolves, for every check instance in the given results, the noisy value published most
   * recently and the raw count that noise was drawn for (the anchor), from one projection query
   * over the report history. See {@link NoiseAnchors} for how the anchor is reconstructed exactly.
   */
  private Map<CheckInstanceKey, NoiseAnchors.PreviousNoise<Double>> previousNoiseByCheckAndStratum(
      List<Result> currentResults) {
    Set<CheckInstanceKey> wanted =
        currentResults.stream()
            .filter(result -> result.getRawValue() != null)
            .map(CheckInstanceKey::of)
            .collect(Collectors.toSet());
    if (wanted.isEmpty()) {
      return Map.of();
    }
    Map<CheckInstanceKey, List<NoiseAnchors.HistoryEntry<Double>>> seriesByKey = new HashMap<>();
    for (ResultHistoryRow row : reportRepository.findResultHistory(ReportStatus.GENERATED)) {
      CheckInstanceKey key = new CheckInstanceKey(row.checkId(), row.stratum());
      if (!wanted.contains(key)) continue;
      seriesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(row.toHistoryEntry());
    }
    Map<CheckInstanceKey, NoiseAnchors.PreviousNoise<Double>> chains = new HashMap<>();
    seriesByKey.forEach(
        (key, series) -> {
          NoiseAnchors.PreviousNoise<Double> previous =
              NoiseAnchors.reconstruct(series, DifferentialPrivacyUtil.NOISE_REFRESH_THRESHOLD);
          if (previous != null) {
            chains.put(key, previous);
          }
        });
    return chains;
  }

  private record CheckInstanceKey(Long checkId, String stratum) {
    private static CheckInstanceKey of(Result result) {
      return new CheckInstanceKey(result.getCheckId(), result.getStratum());
    }
  }

  private Map<Long, Double> getPreferredBudgetByCheckId() {
    return qualityCheckService.findAll().stream()
        .filter(qc -> qc.getEpsilonBudget() != null)
        .collect(Collectors.toMap(QualityCheckDTO::getId, QualityCheckDTO::getEpsilonBudget));
  }

  private void logAllocation(
      double totalEpsilon,
      double totalPreferredEpsilon,
      int totalResults,
      long noPreferenceCount,
      boolean ignorePreferences) {
    log.debug(
        "Epsilon budget: total={}, preferredTotal={}, totalResults={}, noPreferenceCount={}",
        totalEpsilon,
        totalPreferredEpsilon,
        totalResults,
        noPreferenceCount);
    log.debug("Using {} epsilon allocation", ignorePreferences ? "equal" : "preference-aware");
  }
}
