package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.ReportPipeline;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * Regression test for transaction poisoning during report generation: when a step fails through a
 * transactional bean, the inner pipeline transaction is marked rollback-only. The report must still
 * be marked FAILED in a separate transaction — otherwise it would silently stay in GENERATING (with
 * an {@link UnexpectedRollbackException} surfacing instead) and block all future reports.
 */
@SpringBootTest
class ReportPipelineFailureIntegrationTest {

  @Autowired private ReportPipeline reportPipeline;
  @Autowired private ReportRepository reportRepository;

  @MockitoBean private QualityChecksStep qualityChecksStep;

  @BeforeEach
  void setUp() {
    reportRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    reportRepository.deleteAll();
  }

  @Test
  void run_whenStepFailsThroughTransactionalBoundary_reportIsMarkedFailedAndPersisted() {
    when(qualityChecksStep.execute(any()))
        .thenThrow(new DataAccessResourceFailureException("Simulated database failure"));
    Report report = reportRepository.save(new Report());

    reportPipeline.run(report.getId());

    assertThat(reportRepository.findById(report.getId()).orElseThrow().getStatus())
        .isEqualTo(ReportStatus.FAILED);
  }
}
