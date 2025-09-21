package eu.bbmri_eric.quality.agent.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.events.DataQualityCheckResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResultEventHandlerTest {

  @Test
  void onNewReport_setsPatientListCorrectly() {
    ReportRepository mockRepo = mock(ReportRepository.class);
    Report mockReport = new Report();
    mockReport.setStatus(Status.GENERATING);
    when(mockRepo.findAllByStatusIs(Status.GENERATING)).thenReturn(List.of(mockReport));
    ResultEventHandler handler = new ResultEventHandler(mockRepo);
    Set<String> expectedPatients = Set.of("patientA", "patientB", "patientC");
    DataQualityCheckResult event =
        new DataQualityCheckResult(
            this,
            123L,
            "Patient List Check",
            3,
            expectedPatients,
            null,
            LocalDateTime.now(),
            5,
            10,
            0.2f,
            null);

    doAnswer(
            invocation -> {
              Report saved = invocation.getArgument(0);
              Result result = saved.getResults().iterator().next();
              assertEquals(expectedPatients, result.getPatients());
              return null;
            })
        .when(mockRepo)
        .save(any(Report.class));
    handler.onNewReport(event);
    verify(mockRepo, times(1)).save(any(Report.class));
  }
}
