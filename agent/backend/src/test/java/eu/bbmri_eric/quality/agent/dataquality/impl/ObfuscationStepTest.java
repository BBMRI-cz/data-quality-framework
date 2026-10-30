package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.impl.SettingsRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
class ObfuscationStepTest {

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
  void getOrder_shouldReturn20() {
    assertThat(step.getOrder()).isEqualTo(20);
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
    check1.setEpsilonBudget(0.3);
    qualityCheckRepository.save(check1);

    QualityCheck check2 = new QualityCheck("c2", "Check 2", "query2");
    check2.setEpsilonBudget(0.5);
    qualityCheckRepository.save(check2);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", check2.getId(), 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    assertThat(r1.getEpsilon()).isEqualTo(0.3);
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
  void execute_withPreferencesConsumingFullCheckBudget_shouldUsePreferredValues() {
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

    assertThat(r1.getEpsilon()).isEqualTo(1.0);
    assertThat(r2.getEpsilon()).isEqualTo(0.6);
  }

  @Test
  void execute_withPreferencesStarvingOtherResults_shouldThrow() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(1.0).build());

    QualityCheck check1 = new QualityCheck("c1", "Check 1", "query1");
    check1.setEpsilonBudget(0.8);
    qualityCheckRepository.save(check1);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", 999L, 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    assertThatThrownBy(() -> step.execute(report))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("leaving no epsilon");
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
    older.setGeneratedAt(LocalDateTime.now().minusDays(2));
    reportRepository.save(older);
    Report newer = generatedReportWithResult(1L, null, 103, 95.5, 0.5);
    newer.setGeneratedAt(LocalDateTime.now().minusDays(1));
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
    LocalDateTime tied = LocalDateTime.now().minusDays(1);
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
    report.setGeneratedAt(LocalDateTime.now().minusDays(daysAgo));
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
}
