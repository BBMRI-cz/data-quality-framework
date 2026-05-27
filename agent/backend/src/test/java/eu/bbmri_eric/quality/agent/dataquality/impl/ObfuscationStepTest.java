package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.impl.SettingsRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
class ObfuscationStepTest {

  @Autowired private SettingsService settingsService;
  @Autowired private SettingsRepository settingsRepository;
  @Autowired private ObfuscationStep step;
  @Autowired private ReportRepository reportRepository;

  @Test
  void execute_withAllNonNullRawValues_shouldSetObfuscatedValues() {
    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0f, null, null);
    Result r2 = new Result("c2", 2L, 200, null, 150, 100, 1.0f, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    Report result = step.execute(report);

    assertThat(result).isSameAs(report);
    assertThat(r1.getObfuscatedValue()).isNotNull().isNotNegative();
    assertThat(r2.getObfuscatedValue()).isNotNull().isNotNegative();
  }

  @Test
  void execute_withNullRawValues_shouldNotSetObfuscatedValues() {
    Report report = new Report();
    Result r1 = new Result("c1", 1L, null, null, 80, 50, 1.0f, null, null);
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
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0f, null, null);
    Result r2 = new Result("c2", 2L, null, null, 150, 100, 1.0f, null, null);
    Result r3 = new Result("c3", 3L, 300, null, 200, 150, 1.0f, null, null);
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
    assertThat(report.getResults().getFirst().getEpsilon()).isEqualTo(1.0F);
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
                    && r.getEpsilon() == 0.1F);
  }

  @Test
  void execute_shouldNotOverwritePreExistingObfuscatedValueWhenRawIsNull() {
    Report report = new Report();
    Result r1 = new Result("c1", 1L, null, 42.0, 80, 50, 1.0f, null, null);
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
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0f, null, null);
    Result r2 = new Result("c2", 2L, 200, null, 150, 100, 1.0f, null, null);
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
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0f, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    assertThrows(IllegalArgumentException.class, () -> step.execute(report));
  }

  @Test
  void execute_shouldObfuscateAllResultsIndependently() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(100.0).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0f, null, null);
    Result r2 = new Result("c2", 2L, 100, null, 150, 100, 1.0f, null, null);
    report.setResults(new ArrayList<>(List.of(r1, r2)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isNotEqualTo(r2.getObfuscatedValue());
  }

  @Test
  void execute_withLargeEpsilon_shouldBeCloseToRawValue() {
    settingsService.updateSettings(SettingsDTO.builder().epsilon(1000000.0).build());

    Report report = new Report();
    Result r1 = new Result("c1", 1L, 100, null, 80, 50, 1.0f, null, null);
    report.setResults(new ArrayList<>(List.of(r1)));

    step.execute(report);

    assertThat(r1.getObfuscatedValue()).isBetween(90.0, 110.0);
  }
}
