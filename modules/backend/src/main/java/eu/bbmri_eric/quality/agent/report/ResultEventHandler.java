package eu.bbmri_eric.quality.agent.report;

import eu.bbmri_eric.quality.agent.events.CheckResultEvent;
import jakarta.transaction.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class ResultEventHandler {

  private static final Logger log = LoggerFactory.getLogger(ResultEventHandler.class);
  private final ReportRepository reportRepository;

  ResultEventHandler(ReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  @EventListener
  @Transactional
  void onNewReport(CheckResultEvent event) {
    List<Report> reports = reportRepository.findAllByStatusIs(Status.GENERATING);

    reports.forEach(
        report -> {
          report.addResult(
              new Result(
                  event.getCheckName(),
                  event.getCheckId(),
                  event.getRawValue(),
                  LaplaceNoise.addLaplaceNoise(event.getRawValue(), event.getEpsilon(), 1),
                  event.getWarningThreshold(),
                  event.getErrorThreshold(),
                  event.getEpsilon(),
                  event.getError()));
        });
  }
}
