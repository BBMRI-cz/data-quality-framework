package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReportPipelineExecutorTest {

  private final ReportRepository reportRepository = mock(ReportRepository.class);

  @Test
  void runSteps_executesStepsInOrder() {
    List<String> executed = new ArrayList<>();
    ReportPipelineExecutor executor =
        new ReportPipelineExecutor(
            reportRepository,
            List.of(recordingStep("second", 20, executed), recordingStep("first", 10, executed)));
    Report report = new Report();
    report.setId(1L);
    when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

    executor.runSteps(1L);

    assertThat(executed).containsExactly("first", "second");
    assertThat(report.getStatus()).isNotEqualTo(ReportStatus.FAILED);
  }

  @Test
  void runSteps_whenReportNotFound_throws() {
    ReportPipelineExecutor executor = new ReportPipelineExecutor(reportRepository, List.of());
    when(reportRepository.findById(99L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> executor.runSteps(99L));
  }

  @Test
  void markFailed_setsStatusFailedAndSaves() {
    ReportPipelineExecutor executor = new ReportPipelineExecutor(reportRepository, List.of());
    Report report = new Report();
    report.setId(1L);
    when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

    executor.markFailed(1L);

    assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
    verify(reportRepository).save(report);
  }

  @Test
  void markFailed_whenReportNotFound_doesNothing() {
    ReportPipelineExecutor executor = new ReportPipelineExecutor(reportRepository, List.of());
    when(reportRepository.findById(99L)).thenReturn(Optional.empty());

    executor.markFailed(99L);

    verify(reportRepository, never()).save(any());
  }

  private ReportPipelineStep recordingStep(String name, int order, List<String> executed) {
    return new ReportPipelineStep() {
      @Override
      public Report execute(Report report) {
        executed.add(name);
        return report;
      }

      @Override
      public int getOrder() {
        return order;
      }
    };
  }
}
