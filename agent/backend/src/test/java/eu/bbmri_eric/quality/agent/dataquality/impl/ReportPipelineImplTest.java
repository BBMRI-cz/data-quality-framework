package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class ReportPipelineImplTest {

  private final ReportPipelineExecutor executor = mock(ReportPipelineExecutor.class);
  private final ReportPipelineImpl pipeline = new ReportPipelineImpl(executor);

  @Test
  void run_whenStepsSucceed_doesNotMarkFailed() {
    pipeline.run(1L);

    verify(executor).runSteps(1L);
    verify(executor, never()).markFailed(any());
  }

  @Test
  void run_whenStepsThrow_marksFailedAndDoesNotPropagate() {
    doThrow(new RuntimeException("Simulated step failure")).when(executor).runSteps(1L);

    pipeline.run(1L);

    verify(executor).markFailed(1L);
  }
}
