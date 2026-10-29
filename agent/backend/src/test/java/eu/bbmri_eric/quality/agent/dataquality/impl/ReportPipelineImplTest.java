package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
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

class ReportPipelineImplTest {

  private final ReportRepository reportRepository = mock(ReportRepository.class);

  @Test
  void run_whenStepsSucceed_executesStepsInOrder() {
    List<String> executed = new ArrayList<>();
    ReportPipelineImpl pipeline =
        new ReportPipelineImpl(
            reportRepository,
            List.of(recordingStep("second", 20, executed), recordingStep("first", 10, executed)));
    Report report = new Report();
    report.setId(1L);
    when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

    pipeline.run(1L);

    assertThat(executed).containsExactly("first", "second");
    assertThat(report.getStatus()).isNotEqualTo(ReportStatus.FAILED);
  }

  @Test
  void run_whenStepThrows_marksReportAsFailedAndStops() {
    List<String> executed = new ArrayList<>();
    ReportPipelineImpl pipeline =
        new ReportPipelineImpl(
            reportRepository,
            List.of(
                recordingStep("first", 10, executed),
                failingStep(20),
                recordingStep("third", 30, executed)));
    Report report = new Report();
    report.setId(1L);
    when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

    pipeline.run(1L);

    assertThat(executed).containsExactly("first");
    assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
    verify(reportRepository).save(report);
  }

  @Test
  void run_whenStepThrows_doesNotPropagateException() {
    ReportPipelineImpl pipeline =
        new ReportPipelineImpl(reportRepository, List.of(failingStep(10)));
    Report report = new Report();
    report.setId(1L);
    when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

    pipeline.run(1L);

    verify(reportRepository).save(report);
  }

  @Test
  void run_whenReportNotFound_throws() {
    ReportPipelineImpl pipeline = new ReportPipelineImpl(reportRepository, List.of());
    when(reportRepository.findById(99L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> pipeline.run(99L));
    verify(reportRepository, never()).save(org.mockito.ArgumentMatchers.any());
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

  private ReportPipelineStep failingStep(int order) {
    return new ReportPipelineStep() {
      @Override
      public Report execute(Report report) {
        throw new RuntimeException("Simulated step failure");
      }

      @Override
      public int getOrder() {
        return order;
      }
    };
  }
}
