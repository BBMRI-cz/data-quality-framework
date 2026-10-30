package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Transactional
class EntityCountStepTest {

  @Autowired private EntityCountStep step;

  @MockitoBean private DataStoreFactory dataStoreFactory;
  @MockitoBean private DataStore dataStore;

  @BeforeEach
  void setUp() {
    when(dataStoreFactory.resolveDataStore()).thenReturn(dataStore);
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
  void execute_withNullCounts_shouldStoreNulls() {
    when(dataStore.countPatients()).thenReturn(null);
    when(dataStore.countSecondaryEntities()).thenReturn(null);

    Report report = step.execute(new Report());

    assertThat(report.getNumberOfEntities()).isNull();
    assertThat(report.getNumberOfSecondaryEntities()).isNull();
  }

  @Test
  void execute_doesNotTouchObfuscatedTotals() {
    when(dataStore.countPatients()).thenReturn(4321);
    when(dataStore.countSecondaryEntities()).thenReturn(9876);

    Report report = step.execute(new Report());

    assertThat(report.getObfuscatedNumberOfEntities()).isNull();
    assertThat(report.getObfuscatedNumberOfSecondaryEntities()).isNull();
  }

  @Test
  void getOrder_shouldRunBeforeObfuscation() {
    assertThat(step.getOrder())
        .isEqualTo(eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep.ENTITY_COUNT_ORDER);
  }
}
