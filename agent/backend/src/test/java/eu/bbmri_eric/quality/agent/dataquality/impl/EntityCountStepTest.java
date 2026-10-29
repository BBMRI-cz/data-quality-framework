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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Transactional
class EntityCountStepTest {

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
  void execute_withHighEpsilon_shouldKeepObfuscatedCountsCloseToRawCounts() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(1_000_000.0)
            .minThreshold(0)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4321);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9876);
  }

  @Test
  void execute_withHighMinThreshold_shouldSuppressLowCounts() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(1_000_000.0)
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
  void execute_withGaussianNoise_shouldStoreObfuscatedCounts() {
    settingsService.updateSettings(
        SettingsDTO.builder().epsilon(0.9).noiseMechanism(NoiseMechanism.GAUSSIAN).build());
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

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
            .epsilon(1_000_000.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    when(dataStore.countPatients()).thenReturn(4332);
    when(dataStore.countSecondaryEntities()).thenReturn(9885);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4332);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9885);
  }

  @Test
  void execute_withPartiallyChangedTotals_reusesOnlyUnchangedTotal() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(1_000_000.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    when(dataStore.countPatients()).thenReturn(4323);
    when(dataStore.countSecondaryEntities()).thenReturn(9881);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4330);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9881);
  }

  @Test
  void execute_whenPreviousTotalsHaveNoNoisyValue_appliesFreshNoise() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(1_000_000.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    givenPreviousReportWithTotals(4321, 9876, null, null);
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4321);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9876);
  }

  @Test
  void execute_anchorIsDerivedFromHistoryAcrossReuseHop() {
    settingsService.updateSettings(
        SettingsDTO.builder()
            .epsilon(1_000_000.0)
            .minThreshold(10)
            .noiseMechanism(NoiseMechanism.LAPLACE)
            .build());
    // Chain: noise was drawn for 4321; the latest report (4323) reused it. The derived anchor is
    // 4321, so a drift of 6 triggers fresh noise even though the drift from the latest count is 4.
    Report older = givenPreviousReportWithTotals(4321, 9876, 4330, 9880);
    older.setGeneratedAt(LocalDateTime.now().minusDays(2));
    reportRepository.save(older);
    Report newer = givenPreviousReportWithTotals(4323, 9876, 4330, 9880);
    newer.setGeneratedAt(LocalDateTime.now().minusDays(1));
    reportRepository.save(newer);
    when(dataStore.countPatients()).thenReturn(4327);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isNotEqualTo(4330);
    assertThat(report.getObfuscatedNumberOfEntities()).isEqualTo(4327);
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isEqualTo(9880);
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
