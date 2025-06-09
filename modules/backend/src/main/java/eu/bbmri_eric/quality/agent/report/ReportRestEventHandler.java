package eu.bbmri_eric.quality.agent.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
class ReportRestEventHandler {

  private static final Logger log = LoggerFactory.getLogger(ReportRestEventHandler.class);
  ApplicationEventPublisher publisher;

  public ReportRestEventHandler(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @HandleAfterCreate
  public void onAfterCreate(Report report) {
    log.info("✅ Report was created and persisted: {}", report.getId());
    publisher.publishEvent(new NewReportEvent(this, report.getId()));
  }
}
