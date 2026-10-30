package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Transactional
class EntityCountStepTest {

  private static final int STATISTICAL_SAMPLES = 10_000;
  private static final int RAW_COUNT = 10_000;

  @Autowired private EntityCountStep step;
  @Autowired private SettingsService settingsService;
  @Autowired private ReportRepository reportRepository;

  @MockitoBean private DataStoreFactory dataStoreFactory;
  @MockitoBean private DataStore dataStore;

  @BeforeEach
  void setUp() {
    when(dataStoreFactory.resolveDataStore()).thenReturn(dataStore);
    reportRepository.deleteAll();
  }

  @Test
  void execute_shouldStoreRawCounts() {
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getNumberOfEntities()).isEqualTo(4321);
    assertThat(report.getNumberOfSecondaryEntities()).isEqualTo(9876);
  }

  @Test
  void execute_shouldStoreObfuscatedCounts() {
    settingsService.updateSettings(
        SettingsDTO.builder().epsilon(1.0).noiseMechanism(NoiseMechanism.LAPLACE).build());
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isNotNull().isNotNegative();
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isNotNull().isNotNegative();
  }

  @Test
  void execute_withMaxValidEpsilon_shouldKeepObfuscatedCountsCloseToRawCounts() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    // Per-total ε = 10 × 0.1 = 1 → patient noise b = 1, sample noise b = 20
    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4321 - 10, 4321 + 10);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isBetween(9876 - 250, 9876 + 250);
  }

  @Test
  void execute_withHighMinThreshold_shouldSuppressLowCounts() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10000)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    when(dataStore.countPatients()).thenReturn(100);
    when(dataStore.countSecondaryEntities()).thenReturn(200);

    Report report = step.execute(new Report());

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
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    // Totals are always Laplace; the Gaussian path is only for check results. Laplace with
    // ε = 0.9 per total is far tighter than the ≈1 200-sample Gaussian sigma would be.
    assertThat(report.getObfuscatedNumberOfEntities()).isNotNull().isNotNegative();
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isNotNull().isNotNegative();
  }

  @Test
  void execute_withNullCounts_shouldLeaveObfuscatedCountsNull() {
    when(dataStore.countPatients()).thenReturn(null);
    when(dataStore.countSecondaryEntities()).thenReturn(null);

    Report report = step.execute(new Report());

    assertThat(report.getNumberOfEntities()).isNull();
    assertThat(report.getNumberOfSecondaryEntities()).isNull();
    assertThat(report.getObfuscatedNumberOfEntities()).isNull();
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isNull();
  }

  @Test
  void execute_withUnchangedTotals_reusesPreviousNoisyTotals() {
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4330);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withTotalDriftBelowThreshold_reusesNoisyTotals() {
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    when(dataStore.countPatients()).thenReturn(4323);
    when(dataStore.countSecondaryEntities()).thenReturn(9880);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4330);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withChangedTotals_appliesFreshNoise() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    // Patient threshold is 5 (sensitivity 1), sample threshold is 100 (5 x sensitivity 20).
    when(dataStore.countPatients()).thenReturn(4380);
    when(dataStore.countSecondaryEntities()).thenReturn(10100);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4360, 4400);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isBetween(9900, 10300);
  }

  @Test
  void execute_withPartiallyChangedTotals_reusesOnlyUnchangedTotal() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    // Patients moved past their threshold (59 >= 5) -> fresh; samples moved 4 (< 100) -> reuse.
    when(dataStore.countPatients()).thenReturn(4380);
    when(dataStore.countSecondaryEntities()).thenReturn(9880);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4360, 4400);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withSampleDriftBelowHundred_reusesPreviousTotal() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    // 60 additional samples stay below the scaled sample threshold (100) -> invisible.
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9936);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4330);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_whenPreviousTotalsHaveNoNoisyValue_appliesFreshNoise() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    givenPreviousReportWithTotals(4321, 9876, null, null);
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4311, 4331);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isBetween(9626, 10126);
  }

  @Test
  void execute_anchorIsDerivedFromHistoryAcrossReuseHop() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    // Chain: noise was drawn for 4321; the latest report (4323) reused it. The derived anchor is
    // 4321, so a drift of 39 triggers fresh noise even though the drift from the latest count
    // would still have been above the patient threshold of 5.
    Report older = givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    older.setGeneratedAt(LocalDateTime.now().minusDays(2));
    reportRepository.save(older);
    Report newer = givenPreviousReportWithTotals(4323, 9876, 4330, 9880);
    newer.setGeneratedAt(LocalDateTime.now().minusDays(1));
    reportRepository.save(newer);
    when(dataStore.countPatients()).thenReturn(4360);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isBetween(4340, 4380);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withLaplaceDefaults_patientTotalNoiseMatchesSensitivity1() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(3.0)
            .delta(1e-8)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    when(dataStore.countPatients()).thenReturn(RAW_COUNT);
    when(dataStore.countSecondaryEntities()).thenReturn(RAW_COUNT);

    double mae = meanAbsoluteError(Report::getObfuscatedNumberOfEntities);

    // Laplace per-total ε = 3 × 0.2 / 2 = 0.3 → theory MAE = b = 1 / 0.3 ≈ 3.3
    assertThat(mae).isBetween(2.7, 4.0);
  }

  @Test
  void execute_withLaplaceDefaults_sampleTotalNoiseMatchesSensitivity20() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(3.0)
            .delta(1e-8)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    when(dataStore.countPatients()).thenReturn(RAW_COUNT);
    when(dataStore.countSecondaryEntities()).thenReturn(RAW_COUNT);

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
    when(dataStore.countPatients()).thenReturn(RAW_COUNT);
    when(dataStore.countSecondaryEntities()).thenReturn(RAW_COUNT);

    double mae = meanAbsoluteError(Report::getObfuscatedNumberOfSecondaryEntities);

    // Totals always use Laplace: per-total ε = 0.1 → theory MAE = b = 20 / 0.1 = 200.
    // The Gaussian path would produce a much larger error (≈ 977) and is ruled out by this bound.
    assertThat(mae).isBetween(170.0, 230.0);
  }

  /**
   * Measures the mean absolute error of fresh-noise totals over many executions. No previous report
   * is saved, so every execution samples independent fresh noise — keeping the estimate stable
   * enough to assert tight bounds.
   */
  private double meanAbsoluteError(ToIntFunction<Report> noisyTotal) {
    double sum = 0;
    for (int i = 0; i < STATISTICAL_SAMPLES; i++) {
      sum += Math.abs(noisyTotal.applyAsInt(step.execute(new Report())) - RAW_COUNT);
    }
    return sum / STATISTICAL_SAMPLES;
  }

  @Test
  void execute_withRoundingCollisionRun_replaysAnchorToLastFreshDraw() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    // Two consecutive fresh integer draws rounded to the same published value; the anchor must be
    // the raw of the most recent fresh draw (306): drift from it is 4 → reuse; drift from the
    // run's oldest raw (300) would be 10 → fresh.
    Report older = givenPreviousReportWithTotals(300, 9876, 302, 9880);
    older.setGeneratedAt(LocalDateTime.now().minusDays(2));
    reportRepository.save(older);
    Report newer = givenPreviousReportWithTotals(306, 9876, 302, 9880);
    newer.setGeneratedAt(LocalDateTime.now().minusDays(1));
    reportRepository.save(newer);
    when(dataStore.countPatients()).thenReturn(310);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(302);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
  }

  @Test
  void execute_withPreviousTotalBelowCurrentThreshold_drawsFreshInsteadOfRepublishing() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(10.0)
            .minThreshold(50)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    givenPreviousReportWithTotals(40, 60, 40, 60);
    when(dataStore.countPatients()).thenReturn(40);
    when(dataStore.countSecondaryEntities()).thenReturn(60);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(0);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(60);
  }

  @Test
  void findTotalsHistory_withTiedTimestamps_ordersByIdDescending() {
    LocalDateTime tied = LocalDateTime.now().minusDays(1);
    Report older = givenPreviousReportWithTotals(111, 333, 111, 333);
    older.setGeneratedAt(tied);
    reportRepository.save(older);
    Report newer = givenPreviousReportWithTotals(222, 444, 222, 444);
    newer.setGeneratedAt(tied);
    reportRepository.save(newer);

    List<TotalsHistoryRow> rows = reportRepository.findTotalsHistory(ReportStatus.GENERATED);

    assertThat(rows.getFirst().numberOfEntities()).isEqualTo(222);
  }

  @Test
  void getOrder_shouldReturn30() {
    assertThat(step.getOrder()).isEqualTo(30);
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
}
