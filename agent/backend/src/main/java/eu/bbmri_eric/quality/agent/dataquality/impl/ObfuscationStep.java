package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** This pipeline step takes care of adding noise to individual report results. */
@Slf4j
@Component
class ObfuscationStep implements ReportPipelineStep {
  private final SettingsService settingsService;
  private final QualityCheckService qualityCheckService;

  ObfuscationStep(SettingsService settingsService, QualityCheckService qualityCheckService) {
    this.settingsService = settingsService;
    this.qualityCheckService = qualityCheckService;
  }

  private static List<Result> resultsWithNonNullValues(List<Result> results) {
    return results.stream().filter(r -> r.getRawValue() != null).toList();
  }

  private static double computeTotalPreferredEpsilon(
      List<Result> results, Map<Long, Float> preferredBudgetByCheckId) {
    return results.stream()
        .mapToDouble(r -> preferredBudgetByCheckId.getOrDefault(r.getCheckId(), 0.0f))
        .sum();
  }

  private static long countResultsWithoutPreference(
      List<Result> results, Map<Long, Float> preferredBudgetByCheckId) {
    return results.stream()
        .filter(r -> !preferredBudgetByCheckId.containsKey(r.getCheckId()))
        .count();
  }

  private static double computeBaseEpsilon(
      double totalEpsilon,
      double totalPreferredEpsilon,
      int resultCount,
      long noPreferenceCount,
      boolean ignorePreferences) {
    if (ignorePreferences) return totalEpsilon / resultCount;
    return noPreferenceCount > 0
        ? (totalEpsilon - totalPreferredEpsilon) / noPreferenceCount
        : 0;
  }

  private static double resolveEpsilon(
      Result result,
      Map<Long, Float> preferredBudgetByCheckId,
      double baseEpsilon,
      boolean ignorePreferences) {
    if (ignorePreferences) return baseEpsilon;
    return preferredBudgetByCheckId.getOrDefault(result.getCheckId(), (float) baseEpsilon);
  }

  private static void applyNoise(Result result, double epsilon) {
    double noisyValue =
        DifferentialPrivacyUtil.addLaplaceNoise(result.getRawValue(), epsilon, 1);
    result.setObfuscatedValue(noisyValue);
    result.setEpsilon((float) epsilon);
  }

  @Override
  public Report execute(Report report) {
    log.info("Adding obfuscated values for report id: {}", report.getId());
    obfuscateResults(report);
    log.info("Completed obfuscation for report id: {}", report.getId());
    return report;
  }

  private void obfuscateResults(Report report) {
    List<Result> results = report.getResults();
    double totalEpsilon = settingsService.getSettings().getEpsilon();
    Map<Long, Float> preferredBudgetByCheckId = getPreferredBudgetByCheckId();
    List<Result> resultsToObfuscate = resultsWithNonNullValues(results);

    if (resultsToObfuscate.isEmpty()) return;

    double totalPreferredEpsilon =
        computeTotalPreferredEpsilon(resultsToObfuscate, preferredBudgetByCheckId);
    boolean ignorePreferences = totalPreferredEpsilon > totalEpsilon;
    log.warn(String.valueOf(totalPreferredEpsilon));
    log.warn(String.valueOf(totalEpsilon));
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
        totalEpsilon, totalPreferredEpsilon, resultsToObfuscate.size(), noPreferenceCount,
        ignorePreferences);
    for (Result result : results) {
      if (result.getRawValue() == null) continue;
      double epsilon =
          resolveEpsilon(result, preferredBudgetByCheckId, baseEpsilon, ignorePreferences);
      applyNoise(result, epsilon);
    }
  }

  private Map<Long, Float> getPreferredBudgetByCheckId() {
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
        totalEpsilon, totalPreferredEpsilon, totalResults, noPreferenceCount);
    log.debug("Using {} epsilon allocation", ignorePreferences ? "equal" : "preference-aware");
  }

  @Override
  public int getOrder() {
    return 20;
  }
}
