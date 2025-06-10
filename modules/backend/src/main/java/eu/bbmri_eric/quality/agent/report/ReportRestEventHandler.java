package eu.bbmri_eric.quality.agent.report;

import eu.bbmri_eric.quality.agent.events.FinishedReportEvent;
import eu.bbmri_eric.quality.agent.events.NewReportEvent;
import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
class ReportRestEventHandler {

  private static final Logger log = LoggerFactory.getLogger(ReportRestEventHandler.class);
  private final ApplicationEventPublisher publisher;
  private final ReportRepository reportRepository;
  private final FHIRStore fhirStore;

  public ReportRestEventHandler(
      ApplicationEventPublisher publisher, ReportRepository reportRepository, FHIRStore fhirStore) {
    this.publisher = publisher;
    this.reportRepository = reportRepository;
    this.fhirStore = fhirStore;
  }

  @HandleAfterCreate
  public void onAfterCreate(Report report) {
    publisher.publishEvent(new NewReportEvent(this, report.getId()));
    int count = fhirStore.countResources("Patient");
    log.info("Found {} patients", count);
    report.setNumberOfEntities(count);
    reportRepository.save(report);
  }

  @EventListener
  void onFinished(FinishedReportEvent event) {
    reportRepository
        .findById(event.getReportId())
        .ifPresent(
            report -> {
              report.setStatus(Status.GENERATED);
              reportRepository.save(report);
              log.info("✅ Report {} has been generated", report.getId());
            });
  }
}
