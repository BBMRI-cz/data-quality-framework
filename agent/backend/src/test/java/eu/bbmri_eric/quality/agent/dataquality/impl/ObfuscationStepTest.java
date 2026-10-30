package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.privacy.ResultHistoryRow;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.impl.SettingsRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
@Transactional
class ObfuscationStepTest {

  /** Deterministic randomness for the statistical fresh-noise tests in this class. */
  @TestConfiguration
  static class SeededRandomnessConfiguration {
    @Bean
    @Primary
    RandomGenerator seededRandomGenerator() {
      return new SplittableRandom(20261030L);
    }
  }

  @Autowired private SettingsService settingsService;
  @Autowired private SettingsRepository settingsRepository;
  @Autowired private ObfuscationStep step;

  @BeforeEach
  void setUp() {
    settingsRepository.save(new Settings("noiseMechanism", "LAPLACE"));
    reportRepository.deleteAll();
  }

  @Autowired private ReportRepository reportRepository;
  @Autowired private QualityCheckRepository qualityCheckRepository;

  @Test
  void execute_withAllNonNullRawValues_shouldSetObfuscatedValues() {
    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0, null, null);
    Result r2 = new Result("c2", 2L, 200, null, 150, 100, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    Report result = step.execute(report);

    assertThat(result).isSameAs(report);
    assertThat(r1.getObfuscatedValue()).isNotNull().isNotNegative();
    assertThat(r2.getObfuscatedValue()).isNotNull().isNotNegative();
  }

  @Test
  void execute_withNullRawValues_shouldNotSetObfuscatedValues() {
    Report report = new Report();
    Result r1 = new Result("c1", 1L, null, null, 80, 50, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isNull();
  }

  @Test
  void execute_withEmptyResults_shouldNotFail() {
    Report report = new Report();
    report.setResults(new ArrayList<>());

    Report result = step.execute(report);

    assertThat(result).isSameAs(report);
  }

  @Test
  void execute_withMixedNullAndNonNull_shouldOnlyObfuscateNonNull() {
    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0, null, null);
    Result r2 = new Result("c2", 2L, null, null, 150, 100, 1.0, null, null);
    Result r3 = new Result("c3", 3L, 300, null, 200, 150, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2, r3)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isNotNull().isNotNegative();
    assertThat(r2.getObfuscatedValue()).isNull();
    assertThat(r3.getObfuscatedValue()).isNotNull().isNotNegative();
  }

  @Test
  void execute_withSingleResult_shouldUseCheckResultsShareOfEpsilon() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(1.0).build());
    Report report = new Report();
    Result r1 = new Result("c1", 1L, 50, null, 40, 30, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));
    step.execute(report);
    assertThat(r1.getObfuscatedValue()).isNotNull().isNotNegative();
    assertThat(report.getResults().getFirst().getEpsilon()).isEqualTo(0.8);
  }

  @Test
  void execute_withManyResults_shouldHandleAll() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(10.0).build());

    Report report = new Report();
    List<Result> results = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      results.add(new Result("c" + i, (long) i, i * 10, null, 80, 50, null, null, null));
    }
    report.setResults(results);

    step.execute(report);

    assertThat(report.getResults())
        .allMatch(
            r ->
                r.getObfuscatedValue() != null
                    && r.getObfuscatedValue() >= 0
                    && r.getEpsilon() == 0.08);
  }

  @Test
  void execute_shouldNotOverwritePreExistingObfuscatedValueWhenRawIsNull() {
    Report report = new Report();
    Result r1 = new Result("c1", 1L, null, 42.0, 80, 50, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));
    step.execute(report);
    assertThat(r1.getObfuscatedValue()).isEqualTo(42.0);
  }

  @Test
  void getOrder_shouldRunAfterEntityCount() {
    assertThat(step.getOrder())
        .isEqualTo(eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep.OBFUSCATION_ORDER);
  }

  @Test
  void execute_withPersistedReport_shouldPersistObfuscatedValues() {
    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0, null, null);
    Result r2 = new Result("c2", 2L, 200, null, 150, 100, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));
    report = reportRepository.save(report);

    step.execute(report);

    Report saved = reportRepository.findById(report.getId()).orElseThrow();
    assertThat(saved.getResults())
        .allMatch(r -> r.getObfuscatedValue() != null && r.getObfuscatedValue() >= 0);
  }

  @Test
  void execute_withMinimumEpsilonBudget_producesValidNoise() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(0.1).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    // The smallest allowed epsilon still produces a valid noisy value; ε = 0 can no longer be
    // configured at all (rejected by settings validation).
    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isNotNull().isNotNegative();
    assertThat(r1.getEpsilon()).isCloseTo(0.08, within(1e-9));
  }

  @Test
  void execute_shouldObfuscateAllResultsIndependently() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(10.0).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0, null, null);
    Result r2 = new Result("c2", 2L, 100, null, 150, 100, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isNotEqualTo(r2.getObfuscatedValue());
  }

  @Test
  void execute_withLargeEpsilon_shouldBeCloseToRawValue() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(10.0).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isBetween(90.0, 110.0);
  }

  @Test
  void execute_withPreferencesWithinBudget_shouldUsePreferredEpsilon() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(1.0).build());

    QualityCheck check1 = new QualityCheck("c1", "Check 1", "query1");
    check1.setEpsilonBudget(0.25);
    qualityCheckRepository.save(check1);

    QualityCheck check2 = new QualityCheck("c2", "Check 2", "query2");
    check2.setEpsilonBudget(0.5);
    qualityCheckRepository.save(check2);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", check2.getId(), 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    // Sum of preferences (0.75) stays strictly below the check budget (0.8 of the report ε), so
    // the per-check preferences are honored.
    assertThat(r1.getEpsilon()).isEqualTo(0.25);
    assertThat(r2.getEpsilon()).isEqualTo(0.5);
  }

  @Test
  void execute_withPreferencesExceedingBudget_shouldUseEqualSplit() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(2.0).build());

    QualityCheck check1 = new QualityCheck("c1", "Check 1", "query1");
    check1.setEpsilonBudget(1.5);
    qualityCheckRepository.save(check1);

    QualityCheck check2 = new QualityCheck("c2", "Check 2", "query2");
    check2.setEpsilonBudget(1.0);
    qualityCheckRepository.save(check2);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", check2.getId(), 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    assertThat(r1.getEpsilon()).isEqualTo(0.8);
    assertThat(r2.getEpsilon()).isEqualTo(0.8);
  }

  @Test
  void execute_withMixedPreferencesWithinBudget_shouldDistributeRemainder() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(1.0).build());

    QualityCheck check1 = new QualityCheck("c1", "Check 1", "query1");
    check1.setEpsilonBudget(0.4);
    qualityCheckRepository.save(check1);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", 999L, 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    assertThat(r1.getEpsilon()).isEqualTo(0.4);
    assertThat(r2.getEpsilon()).isEqualTo(0.4);
  }

  @Test
  void execute_withAllResultsHavingPreferencesWithinBudget_shouldUsePreferredValues() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(2.0).build());

    QualityCheck check1 = new QualityCheck("c1", "Check 1", "query1");
    check1.setEpsilonBudget(0.8);
    qualityCheckRepository.save(check1);

    QualityCheck check2 = new QualityCheck("c2", "Check 2", "query2");
    check2.setEpsilonBudget(0.7);
    qualityCheckRepository.save(check2);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", check2.getId(), 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    assertThat(r1.getEpsilon()).isEqualTo(0.8);
    assertThat(r2.getEpsilon()).isEqualTo(0.7);
  }

  @Test
  void execute_withPreferencesConsumingWholeCheckBudget_fallsBackToEqualSplit() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(2.0).build());

    QualityCheck check1 = new QualityCheck("c1", "Check 1", "query1");
    check1.setEpsilonBudget(1.0);
    qualityCheckRepository.save(check1);

    QualityCheck check2 = new QualityCheck("c2", "Check 2", "query2");
    check2.setEpsilonBudget(0.6);
    qualityCheckRepository.save(check2);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", check2.getId(), 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    // Preferences take up the whole check budget (1.6 of the report ε): they are ignored and the
    // budget is shared equally so no check is starved to an epsilon of 0.
    assertThat(r1.getEpsilon()).isEqualTo(0.8);
    assertThat(r2.getEpsilon()).isEqualTo(0.8);
  }

  @Test
  void execute_withPreferencesConsumingWholeBudget_fallsBackToEqualSplit() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(1.0).build());

    QualityCheck check1 = new QualityCheck("c1", "Check 1", "query1");
    check1.setEpsilonBudget(0.8);
    qualityCheckRepository.save(check1);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", 999L, 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    // Preferences exactly equal to the whole check budget (0.8 of the report ε) are ignored:
    // the remaining check must not be starved with an epsilon of 0.
    step.execute(report);

    assertThat(r1.getEpsilon()).isEqualTo(0.4);
    assertThat(r2.getEpsilon()).isEqualTo(0.4);
    assertThat(r1.getObfuscatedValue()).isNotNull();
    assertThat(r2.getObfuscatedValue()).isNotNull();
  }

  @Test
  void execute_withUnchangedRawValue_reusesPreviousObfuscatedValue() {
    generatedReportWithResult(1L, null, 100, 95.5, 0.5);

    Report report = new Report();
    Result current = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(current)));

    step.execute(report);

    assertThat(current.getObfuscatedValue()).isEqualTo(95.5);
    assertThat(current.getEpsilon()).isEqualTo(0.5);
  }

  @Test
  void execute_withRawValueDriftBelowThreshold_reusesPreviousObfuscatedValue() {
    generatedReportWithResult(1L, null, 100, 95.5, 0.5);

    Report report = new Report();
    Result current = new Result("c1", 1L, 103, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(current)));

    step.execute(report);

    assertThat(current.getObfuscatedValue()).isEqualTo(95.5);
    assertThat(current.getEpsilon()).isEqualTo(0.5);
  }

  @Test
  void execute_withRawValueDriftAtThreshold_appliesFreshNoise() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    generatedReportWithResult(1L, null, 100, 95.5, 0.5);

    Report report = new Report();
    Result current = new Result("c1", 1L, 105, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(current)));

    step.execute(report);

    assertThat(current.getObfuscatedValue()).isNotNull().isNotEqualTo(95.5);
  }

  @Test
  void execute_withChangedRawValue_appliesFreshNoise() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    generatedReportWithResult(1L, null, 100, 95.5, 0.5);

    Report report = new Report();
    Result current = new Result("c1", 1L, 106, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(current)));

    step.execute(report);

    assertThat(current.getObfuscatedValue()).isNotNull().isNotEqualTo(95.5);
  }

  @Test
  void execute_withSameRawValueButDifferentStratum_appliesFreshNoise() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    generatedReportWithResult(1L, "stratum_A", 100, 95.5, 0.5);

    Report report = new Report();
    Result differentStratum = new Result("c1", 1L, 100, null, 80, 50, null, null, "stratum_B");
    Result sameStratum = new Result("c1", 1L, 100, null, 80, 50, null, null, "stratum_A");
    report.setResults(new ArrayList<>(List.of(differentStratum, sameStratum)));

    step.execute(report);

    assertThat(differentStratum.getObfuscatedValue()).isNotNull().isNotEqualTo(95.5);
    assertThat(sameStratum.getObfuscatedValue()).isEqualTo(95.5);
    assertThat(sameStratum.getEpsilon()).isEqualTo(0.5);
  }

  @Test
  void execute_whenPreviousResultHasNoObfuscatedValue_appliesFreshNoise() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    Report previous = new Report();
    previous.setStatus(ReportStatus.GENERATED);
    previous.addResult(new Result("c1", 1L, 100, null, 80, 50, null, null, null));
    reportRepository.save(previous);

    Report report = new Report();
    Result current = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(current)));

    step.execute(report);

    assertThat(current.getObfuscatedValue()).isNotNull();
    assertThat(current.getEpsilon()).isNotNull();
  }

  @Test
  void execute_anchorIsDerivedFromHistoryAcrossReuseHop() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    // Chain: noise was drawn for raw 100; the latest report (raw 103) reused it. The derived
    // anchor must therefore be 100, so a drift of 6 triggers fresh noise even though the drift
    // from the latest raw value is only 3.
    Report older = generatedReportWithResult(1L, null, 100, 95.5, 0.5);
    older.setGeneratedAt(java.time.LocalDateTime.now().minusDays(2));
    reportRepository.save(older);
    Report newer = generatedReportWithResult(1L, null, 103, 95.5, 0.5);
    newer.setGeneratedAt(java.time.LocalDateTime.now().minusDays(1));
    reportRepository.save(newer);

    Report report = new Report();
    Result current = new Result("c1", 1L, 106, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(current)));

    step.execute(report);

    assertThat(current.getObfuscatedValue()).isNotNull().isNotEqualTo(95.5);
  }

  @Test
  void execute_withReuseChain_keepsValueConstantOverMultipleRuns() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    generatedReportWithResult(1L, null, 100, 97.0, 0.7);

    Report secondRun = new Report();
    Result r2 = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    secondRun.setResults(new ArrayList<>(List.of(r2)));
    step.execute(secondRun);

    assertThat(r2.getObfuscatedValue()).isEqualTo(97.0);
  }

  @Test
  void execute_withSuppressedCollisionRun_replaysAnchorToLastFreshDraw() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    // Every draw published the same value (as suppressed draws all publish 0.0 would); the anchor
    // must replay forward to the last fresh draw (75), not stay at the run's oldest raw (70).
    // Drift from the replayed anchor is 4 → reuse; drift from the oldest raw would be 9 → fresh.
    backdate(generatedReportWithResult(1L, null, 70, 75.0, 0.5), 4);
    backdate(generatedReportWithResult(1L, null, 72, 75.0, 0.5), 3);
    backdate(generatedReportWithResult(1L, null, 75, 75.0, 0.5), 2);
    backdate(generatedReportWithResult(1L, null, 77, 75.0, 0.5), 1);

    Report report = new Report();
    Result current = new Result("c1", 1L, 79, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(current)));

    step.execute(report);

    assertThat(current.getObfuscatedValue()).isEqualTo(75.0);
    assertThat(current.getEpsilon()).isEqualTo(0.5);
  }

  @Test
  void execute_withPreviousValueBelowCurrentThreshold_drawsFreshInsteadOfRepublishing() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    generatedReportWithResult(1L, null, 30, 30.0, 0.5);

    Report report = new Report();
    Result current = new Result("c1", 1L, 30, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(current)));

    step.execute(report);

    assertThat(current.getObfuscatedValue()).isEqualTo(0.0);
  }

  @Test
  void findResultHistory_withTiedTimestamps_ordersByIdDescending() {
    java.time.LocalDateTime tied = java.time.LocalDateTime.now().minusDays(1);
    Report older = generatedReportWithResult(1L, null, 111, 50.0, 0.5);
    older.setGeneratedAt(tied);
    reportRepository.save(older);
    Report newer = generatedReportWithResult(1L, null, 112, 60.0, 0.5);
    newer.setGeneratedAt(tied);
    reportRepository.save(newer);

    List<ResultHistoryRow> rows = reportRepository.findResultHistory(ReportStatus.GENERATED);

    assertThat(rows.getFirst().rawValue()).isEqualTo(112);
  }

  private Report generatedReportWithResult(
      Long checkId, String stratum, int rawValue, double obfuscatedValue, double epsilon) {
    Report previous = new Report();
    previous.setStatus(ReportStatus.GENERATED);
    Result result =
        new Result("c1", checkId, rawValue, obfuscatedValue, 80, 50, epsilon, null, stratum);
    previous.addResult(result);
    return reportRepository.save(previous);
  }

  private Report backdate(Report report, int daysAgo) {
    report.setGeneratedAt(java.time.LocalDateTime.now().minusDays(daysAgo));
    return reportRepository.save(report);
  }

  @Test
  void execute_withHighMinThreshold_shouldSuppressLowCounts() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(10.0).minThreshold(200).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isEqualTo(0.0);
  }

  @Test
  void execute_withMinThresholdNotReached_shouldPreserveCounts() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(10.0).minThreshold(10).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isCloseTo(100.0, within(2.0));
  }

  @Test
  void execute_shouldApplyMinThresholdFromSettings() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(10.0).minThreshold(500).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 300, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isEqualTo(0.0);
  }

  @Test
  void execute_withGaussianNoise_shouldSetObfuscatedValues() {
    settingsService.updateSettings(
        SettingsDTO.builder().epsilon(0.9).noiseMechanism(NoiseMechanism.GAUSSIAN).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", 2L, 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    Report result = step.execute(report);

    assertThat(result).isSameAs(report);
    assertThat(r1.getObfuscatedValue()).isNotNull().isNotNegative();
    assertThat(r2.getObfuscatedValue()).isNotNull().isNotNegative();
  }

  @Test
  void execute_withLaplaceNoise_shouldSetObfuscatedValues() {
    settingsService.updateSettings(
        SettingsDTO.builder().epsilon(10.0).noiseMechanism(NoiseMechanism.LAPLACE).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", 2L, 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    Report result = step.execute(report);

    assertThat(result).isSameAs(report);
    assertThat(r1.getObfuscatedValue()).isNotNull().isNotNegative();
    assertThat(r2.getObfuscatedValue()).isNotNull().isNotNegative();
  }

  @Test
  void execute_withGaussianNoise_shouldBeCloseToRawValue() {
    settingsService.updateSettings(
        SettingsDTO.builder().epsilon(1.0).noiseMechanism(NoiseMechanism.GAUSSIAN).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isBetween(75.0, 125.0);
  }

  // ---- Entity total obfuscation (moved here from the entity count step) ----

  private static final int STATISTICAL_SAMPLES = 10_000;
  private static final int RAW_TOTAL = 10_000;

  private Report newReportWithTotals(Integer patients, Integer secondary) {
    Report report = new Report();
    report.setNumberOfEntities(patients);
    report.setNumberOfSecondaryEntities(secondary);
    return report;
  }

  private Report givenPreviousReportWithTotals(
      int patients, int secondary, Integer noisyPatients, Integer noisySecondary) {
    Report previous = new Report();
    previous.setStatus(ReportStatus.GENERATED);
    previous.setNumberOfEntities(patients);
    previous.setNumberOfSecondaryEntities(secondary);
    previous.setObfuscatedNumberOfEntities(noisyPatients);
    previous.setObfuscatedNumberOfSecondaryEntities(noisySecondary);
    return reportRepository.save(previous);
  }

  @Test
  void execute_storesObfuscatedTotals() {
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);
    Report report = step.execute(newReportWithTotals(4321, 9876));

    assertThat(report.getObfuscatedNumberOfEntities()).isNotNull().isNotNegative();
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isNotNull().isNotNegative();
  }

  @Test
  void execute_withMaxValidEpsilon_keepsTotalsCloseToRawCounts() {
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);
    Report report = step.execute(newReportWithTotals(4321, 9876));

    // Per-total ε = 10 × 0.1 = 1 → patient noise b = 1, sample noise b = 20
    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4321 - 10, 4321 + 10);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isBetween(9876 - 250, 9876 + 250);
  }

  @Test
  void execute_withHighMinThreshold_suppressesLowTotals() {
    whenSettings(10.0, 10_000, NoiseMechanism.LAPLACE);
    Report report = step.execute(newReportWithTotals(100, 200));

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(0);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(0);
  }

  @Test
  void execute_withGaussianMechanism_stillUsesLaplaceForTotals() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(0.9)
            .delta(1e-8)
            .noiseMechanism(NoiseMechanism.GAUSSIAN)
            .build());

    Report report = step.execute(newReportWithTotals(4321, 9876));

    // Totals are always Laplace; at per-total ε = 0.09 the Gaussian σ would be ≈8 000 samples.
    assertThat(report.getObfuscatedNumberOfEntities()).isNotNull().isNotNegative();
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isNotNull().isNotNegative();
  }

  @Test
  void execute_totalsStayNull_whenRawTotalsAreNull() {
    Report report = step.execute(newReportWithTotals(null, null));

    assertThat(report.getObfuscatedNumberOfEntities()).isNull();
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isNull();
  }

  @Test
  void execute_withUnchangedTotals_reusesPreviousNoisyTotals() {
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    Report report = step.execute(newReportWithTotals(4321, 9876));

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4330);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withTotalDriftBelowThreshold_reusesNoisyTotals() {
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    Report report = step.execute(newReportWithTotals(4323, 9880));

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4330);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withChangedTotals_appliesFreshNoise() {
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);
    // Patient threshold is 5 (sensitivity 1), sample threshold is 100 (5 x sensitivity 20).
    Report report = step.execute(newReportWithTotals(4380, 10_100));

    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4360, 4400);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isBetween(9900, 10_300);
  }

  @Test
  void execute_withPartiallyChangedTotals_reusesOnlyUnchangedTotal() {
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);
    // Patients moved past their threshold (59 >= 5) -> fresh; samples moved 4 (< 100) -> reuse.
    Report report = step.execute(newReportWithTotals(4380, 9880));

    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4360, 4400);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withSampleDriftBelowHundred_reusesPreviousTotal() {
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);
    // 60 additional samples stay below the scaled sample threshold (100) -> invisible.
    Report report = step.execute(newReportWithTotals(4321, 9936));

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4330);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_whenPreviousTotalsHaveNoNoisyValue_appliesFreshNoise() {
    givenPreviousReportWithTotals(4321, 9876, null, null);
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);
    Report report = step.execute(newReportWithTotals(4321, 9876));

    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4311, 4331);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isBetween(9626, 10_126);
  }

  @Test
  void execute_anchorIsDerivedFromTotalsHistoryAcrossReuseHop() {
    // Chain: noise was drawn for 4321; the latest report (4323) reused it. The derived anchor is
    // 4321, so a drift of 39 triggers fresh noise.
    Report older = givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    older.setGeneratedAt(java.time.LocalDateTime.now().minusDays(2));
    reportRepository.save(older);
    Report newer = givenPreviousReportWithTotals(4323, 9876, 4330, 9880);
    newer.setGeneratedAt(java.time.LocalDateTime.now().minusDays(1));
    reportRepository.save(newer);
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);

    Report report = step.execute(newReportWithTotals(4360, 9876));

    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4340, 4380);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withRoundingCollisionRun_replaysAnchorToLastFreshDraw() {
    // Two consecutive fresh integer draws rounded to the same published value; the anchor must be
    // the raw of the most recent fresh draw (306): drift from it is 4 → reuse.
    Report older = givenPreviousReportWithTotals(300, 9876, 302, 9880);
    older.setGeneratedAt(java.time.LocalDateTime.now().minusDays(2));
    reportRepository.save(older);
    Report newer = givenPreviousReportWithTotals(306, 9876, 302, 9880);
    newer.setGeneratedAt(java.time.LocalDateTime.now().minusDays(1));
    reportRepository.save(newer);
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);

    Report report = step.execute(newReportWithTotals(310, 9876));

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(302);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withPreviousTotalBelowCurrentThreshold_drawsFreshInsteadOfRepublishing() {
    givenPreviousReportWithTotals(40, 60, 40, 60);
    whenSettings(10.0, 50, NoiseMechanism.LAPLACE);

    Report report = step.execute(newReportWithTotals(40, 60));

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(0);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(60);
  }

  @Test
  void execute_withLaplaceDefaults_patientTotalNoiseMatchesSensitivity1() {
    whenSettings(3.0, 50, NoiseMechanism.LAPLACE);

    double mae = meanAbsoluteError(Report::getObfuscatedNumberOfEntities);

    // Laplace per-total ε = 3 × 0.2 / 2 = 0.3 → theory MAE = b = 1 / 0.3 ≈ 3.3
    assertThat(mae).isBetween(2.7, 4.0);
  }

  @Test
  void execute_withLaplaceDefaults_sampleTotalNoiseMatchesSensitivity20() {
    whenSettings(3.0, 50, NoiseMechanism.LAPLACE);

    double mae = meanAbsoluteError(Report::getObfuscatedNumberOfSecondaryEntities);

    // Laplace per-total ε = 0.3 → theory MAE = b = 20 / 0.3 ≈ 66.7
    assertThat(mae).isBetween(55.0, 80.0);
  }

  @Test
  void execute_withGaussianMechanism_sampleTotalStillMatchesLaplaceSensitivity20() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(1.0)
            .delta(1e-8)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.GAUSSIAN)
            .build());

    double mae = meanAbsoluteError(Report::getObfuscatedNumberOfSecondaryEntities);

    // Totals always use Laplace: per-total ε = 0.1 → theory MAE = b = 20 / 0.1 = 200.
    // The Gaussian path would produce a much larger error (≈ 977) and is ruled out by this bound.
    assertThat(mae).isBetween(170.0, 230.0);
  }

  /** Executions never persist reports, so every call draws independent fresh noise. */
  private double meanAbsoluteError(ToIntFunction<Report> noisyTotal) {
    double sum = 0;
    for (int i = 0; i < STATISTICAL_SAMPLES; i++) {
      sum +=
          Math.abs(
              noisyTotal.applyAsInt(step.execute(newReportWithTotals(RAW_TOTAL, RAW_TOTAL)))
                  - RAW_TOTAL);
    }
    return sum / STATISTICAL_SAMPLES;
  }

  private void whenSettings(double epsilon, int minThreshold, NoiseMechanism mechanism) {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(epsilon)
            .delta(1e-8)
            .minThreshold(minThreshold)
            .noiseMechanism(mechanism)
            .build());
  }
}
