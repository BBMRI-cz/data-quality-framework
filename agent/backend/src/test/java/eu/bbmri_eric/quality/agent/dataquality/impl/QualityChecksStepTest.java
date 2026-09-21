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

  @Test
  void execute_withNoActiveChecks_producesNoResults() {
    when(repository.findAllByActive(true)).thenReturn(new QualityCheck[0]);

    Report report = new Report();
    qualityChecksStep.execute(report);

    verify(repository).findAllByActive(true);
    assertEquals(0, report.getResults().size());
  }
}
