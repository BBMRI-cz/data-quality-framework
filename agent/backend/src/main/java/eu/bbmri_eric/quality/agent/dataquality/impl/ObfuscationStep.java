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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This pipeline step takes care of adding noise to individual report results.
 *
 * <p>To prevent averaging attacks across repeated reports, a result whose raw count is unchanged
 * compared to the same check (and stratum) in the previous generated report reuses the previous
 * noisy value instead of sampling fresh noise. A count that changes and later returns to an earlier
 * value gets fresh noise, so counts that change often are only partly protected.
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
    Map<CheckInstanceKey, Result> previousResults = previousResultsByCheckAndStratum();
    for (Result result : results) {
      if (result.getRawValue() == null) continue;
      Result previous = previousResults.get(CheckInstanceKey.of(result));
      if (previous != null && Objects.equals(previous.getRawValue(), result.getRawValue())) {
        result.setObfuscatedValue(previous.getObfuscatedValue());
        result.setEpsilon(previous.getEpsilon());
        continue;
      }
      double epsilon =
          resolveEpsilon(result, preferredBudgetByCheckId, baseEpsilon, ignorePreferences);
      applyNoise(result, epsilon, noiseMechanism, delta, threshold);
    }
  }

  /**
   * Looks up the results of the previously generated report, indexed by check and stratum, so that
   * unchanged counts can reuse their previous noisy values. Only completed reports count: the
   * single-generation guard keeps this lookup unambiguous. Results without a noisy value are
   * ignored, and first occurrences win over duplicates.
   */
  private Map<CheckInstanceKey, Result> previousResultsByCheckAndStratum() {
    return reportRepository
        .findTopByStatusIsOrderByGeneratedAtDesc(ReportStatus.GENERATED)
        .map(Report::getResults)
        .orElse(List.of())
        .stream()
        .filter(result -> result.getObfuscatedValue() != null)
        .collect(
            Collectors.toMap(CheckInstanceKey::of, result -> result, (first, duplicate) -> first));
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
