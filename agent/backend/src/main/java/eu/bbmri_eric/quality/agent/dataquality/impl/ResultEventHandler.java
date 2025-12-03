package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.event.DataQualityCheckResultEvent;
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
  void onNewResult(DataQualityCheckResultEvent event) {
    List<Report> reports = reportRepository.findAllByStatusIs(ReportStatus.GENERATING);
    reports.forEach(
        report -> {
          double noisyValue =
              DifferentialPrivacyUtil.addLaplaceNoise(event.getRawValue(), event.getEpsilon(), 1);
          Result result =
              new Result(
                  event.getCheckName(),
                  event.getCheckId(),
                  event.getRawValue(),
                  noisyValue,
                  event.getWarningThreshold(),
                  event.getErrorThreshold(),
                  event.getEpsilon(),
                  event.getError(),
                  event.getStratum());
          result.setPatients(event.getPatientSet());
          report.addResult(result);
          reportRepository.save(report);
        });
  }
}
