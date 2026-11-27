package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

class QualityChecksStepTest {

  private QualityCheckRepository repository;
  private QualityChecksStep qualityChecksStep;

  @BeforeEach
  void setUp() {
    repository = mock(QualityCheckRepository.class);
    DataStoreFactory dataStoreFactory = mock(DataStoreFactory.class);
    FHIRServer dataStore = mock(FHIRServer.class);
    when(dataStoreFactory.resolveDataStore()).thenReturn(dataStore);
    qualityChecksStep = new QualityChecksStep(repository, dataStoreFactory, new ModelMapper());
  }

  @Test
  void execute_queriesOnlyActiveChecksAndRunsOnlyThem() {
    QualityCheck activeCheck = new QualityCheck("Active Check", "Runs", "define Test: true");
    activeCheck.setId(1L);
    when(repository.findAllByActive(true)).thenReturn(new QualityCheck[] {activeCheck});

    Report report = new Report();
    qualityChecksStep.execute(report);

    verify(repository).findAllByActive(true);
    verify(repository, never()).findAll();
    assertEquals(1, report.getResults().size());
  }

  /** A persisted-style check whose counting can move its raw count by 2 per patient. */
  static class CustomSensitivityCheck extends QualityCheck {
    CustomSensitivityCheck(Long id) {
      super(id, "Custom", "Overrides sensitivity", "define Test: true");
    }

    @Override
    public int getSensitivity() {
      return 2;
    }
  }

  @Test
  void execute_carriesCheckSensitivityOntoResults() {
    QualityCheck defaultCheck = new QualityCheck("Default", "Uses default", "define Test: true");
    defaultCheck.setId(1L);
    QualityCheck customCheck = new CustomSensitivityCheck(2L);
    when(repository.findAllByActive(true))
        .thenReturn(new QualityCheck[] {defaultCheck, customCheck});

    Report report = new Report();
    qualityChecksStep.execute(report);

    Map<Long, Integer> sensitivityByCheckId =
        report.getResults().stream()
            .collect(Collectors.toMap(Result::getCheckId, Result::getSensitivity));
    assertEquals(Map.of(1L, 1, 2L, 2), sensitivityByCheckId);
  }

  @Test
  void execute_withNoActiveChecks_producesNoResults() {
    when(repository.findAllByActive(true)).thenReturn(new QualityCheck[0]);

    Report report = new Report();
    qualityChecksStep.execute(report);

    verify(repository).findAllByActive(true);
    assertEquals(0, report.getResults().size());
  }
}
