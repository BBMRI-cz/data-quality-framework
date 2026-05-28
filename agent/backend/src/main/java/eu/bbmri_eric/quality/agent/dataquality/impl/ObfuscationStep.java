package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
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
    return noPreferenceCount > 0 ? (totalEpsilon - totalPreferredEpsilon) / noPreferenceCount : 0;
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
      Result result, double epsilon, NoiseMechanism noiseMechanism, double delta) {
    double noisyValue;
    if (noiseMechanism == NoiseMechanism.GAUSSIAN) {
      noisyValue =
          DifferentialPrivacyUtil.addGaussianNoise(result.getRawValue(), epsilon, delta, 1);
    } else {
      noisyValue = DifferentialPrivacyUtil.addLaplaceNoise(result.getRawValue(), epsilon, 1);
    }
    result.setObfuscatedValue(noisyValue);
    result.setEpsilon(epsilon);
  }

  private void obfuscateResults(Report report) {
    log.info("Adding obfuscated values for report id: {}", report.getId());
    List<Result> results = report.getResults();
    var settings = settingsService.getSettings();
    double totalEpsilon = settings.getEpsilon();
    double delta = settings.getDelta();
    NoiseMechanism noiseMechanism = settings.getNoiseMechanism();
    int threshold = settings.getMinThreshold();
    DifferentialPrivacyUtil.setLowCountThreshold(threshold);
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
    for (Result result : results) {
      if (result.getRawValue() == null) continue;
      double epsilon =
          resolveEpsilon(result, preferredBudgetByCheckId, baseEpsilon, ignorePreferences);
      applyNoise(result, epsilon, noiseMechanism, delta);
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
