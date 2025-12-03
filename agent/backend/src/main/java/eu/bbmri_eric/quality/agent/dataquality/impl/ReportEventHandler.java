package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.dataquality.FHIRStore;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.event.DQCheckResultsGatheredEvent;
import eu.bbmri_eric.quality.agent.dataquality.event.NewReportEvent;
import eu.bbmri_eric.quality.agent.dataquality.event.ReportGeneratedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
class ReportEventHandler {

  private final EventPublisher publisher;
  private final ReportRepository reportRepository;
  private final FHIRStore fhirStore;

  public ReportEventHandler(
      EventPublisher publisher, ReportRepository reportRepository, FHIRStore fhirStore) {
    this.publisher = publisher;
    this.reportRepository = reportRepository;
    this.fhirStore = fhirStore;
  }

  @HandleAfterCreate
  @Transactional
  public void onAfterCreate(Report report) {
    publisher.publishEvent(new NewReportEvent(report.getId()));
  }

  @EventListener
  @Transactional
  void onResultsGathered(DQCheckResultsGatheredEvent event) {
    reportRepository
        .findById(event.getReportId())
        .ifPresent(
            report -> {
              int count = fhirStore.countResources("Patient");
              report.setNumberOfEntities(count);
              report.setStatus(ReportStatus.GENERATED);
              publisher.publishEvent(new ReportGeneratedEvent(report.getId()));
            });
  }
}
