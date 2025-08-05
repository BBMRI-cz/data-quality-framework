package eu.bbmri_eric.quality.agent.report;

import eu.bbmri_eric.quality.agent.events.DataQualityCheckResult;
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
  private final PatientRepository patientRepository;

  ResultEventHandler(ReportRepository reportRepository, PatientRepository patientRepository) {
    this.reportRepository = reportRepository;
    this.patientRepository = patientRepository;
  }

  @EventListener
  @Transactional
  void onNewReport(DataQualityCheckResult event) {
    List<Report> reports = reportRepository.findAllByStatusIs(Status.GENERATING);

    List<Patient> patients =
        event.getPatientList().stream()
            .map(id -> patientRepository.findById(id).orElseGet(() -> new Patient(id)))
            .toList();

    List<Patient> newPatients =
        patients.stream().filter(p -> !patientRepository.existsById(p.getId())).toList();
    patientRepository.saveAll(newPatients);

    reports.forEach(
        report -> {
          Result result =
              new Result(
                  event.getCheckName(),
                  event.getCheckId(),
                  event.getRawValue(),
                  DifferentialPrivacyUtil.addLaplaceNoise(
                      event.getRawValue(), event.getEpsilon(), 1),
                  event.getWarningThreshold(),
                  event.getErrorThreshold(),
                  event.getEpsilon(),
                  event.getError(),
                  event.getStratum());

          result.setPatients(patients);

          report.addResult(result);
          reportRepository.save(report);
        });
  }
}
