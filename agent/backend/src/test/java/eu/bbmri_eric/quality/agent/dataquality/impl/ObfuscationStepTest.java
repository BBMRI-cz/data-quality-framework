package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.impl.SettingsRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
  }

  @AfterEach
  void tearDown() {
    DifferentialPrivacyUtil.setLowCountThreshold(10.0);
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
  void execute_withSingleResult_shouldUseFullEpsilon() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(1.0).build());
    Report report = new Report();
    Result r1 = new Result("c1", 1L, 50, null, 40, 30, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));
    step.execute(report);
    assertThat(r1.getObfuscatedValue()).isNotNull().isNotNegative();
    assertThat(report.getResults().getFirst().getEpsilon()).isEqualTo(1.0);
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
                    && r.getEpsilon() == 0.1);
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
  void execute_withZeroEpsilonBudget_shouldProduceZeroObfuscatedValues() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(0.0).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    assertThrows(IllegalArgumentException.class, () -> step.execute(report));
  }

  @Test
  void execute_shouldObfuscateAllResultsIndependently() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(100.0).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0, null, null);
    Result r2 = new Result("c2", 2L, 100, null, 150, 100, 1.0, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isNotEqualTo(r2.getObfuscatedValue());
  }

  @Test
  void execute_withLargeEpsilon_shouldBeCloseToRawValue() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(1000000.0).build());

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

    assertThat(r1.getEpsilon()).isEqualTo(1.0);
    assertThat(r2.getEpsilon()).isEqualTo(1.0);
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
    assertThat(r2.getEpsilon()).isEqualTo(0.6);
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
  void execute_withPreferencesSumEqualToBudget_shouldUsePreferredValues() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(2.0).build());

    QualityCheck check1 = new QualityCheck("c1", "Check 1", "query1");
    check1.setEpsilonBudget(1.2);
    qualityCheckRepository.save(check1);

    QualityCheck check2 = new QualityCheck("c2", "Check 2", "query2");
    check2.setEpsilonBudget(0.8);
    qualityCheckRepository.save(check2);

    Report report = new Report();
    Result r1 = new Result("c1", check1.getId(), 100, null, 80, 50, null, null, null);
    Result r2 = new Result("c2", check2.getId(), 200, null, 150, 100, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    assertThat(r1.getEpsilon()).isEqualTo(1.2);
    assertThat(r2.getEpsilon()).isEqualTo(0.8);
  }

  @Test
  void execute_withHighMinThreshold_shouldSuppressLowCounts() {
    settingsService.updateSettings(
        SettingsDTO.builder().epsilon(1_000_000.0).minThreshold(200).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isEqualTo(0.0);
  }

  @Test
  void execute_withZeroMinThreshold_shouldPreserveCounts() {
    settingsService.updateSettings(
        SettingsDTO.builder().epsilon(1_000_000.0).minThreshold(0).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, null, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isCloseTo(100.0, within(0.1));
  }

  @Test
  void execute_shouldApplyMinThresholdFromSettings() {
    settingsService.updateSettings(
        SettingsDTO.builder().epsilon(1_000_000.0).minThreshold(500).build());

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
        SettingsDTO.builder().epsilon(100.0).noiseMechanism(NoiseMechanism.LAPLACE).build());

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

    assertThat(r1.getObfuscatedValue()).isBetween(80.0, 120.0);
  }
}
