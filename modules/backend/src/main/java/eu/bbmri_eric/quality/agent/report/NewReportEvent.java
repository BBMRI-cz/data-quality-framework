package eu.bbmri_eric.quality.agent.report;

import org.springframework.context.ApplicationEvent;

public class NewReportEvent extends ApplicationEvent {
  private Long reportId;

  public NewReportEvent(Object source) {
    super(source);
  }

  public NewReportEvent(Object source, Long reportId) {
    super(source);
    this.reportId = reportId;
  }

  public Long getReportId() {
    return reportId;
  }
}
