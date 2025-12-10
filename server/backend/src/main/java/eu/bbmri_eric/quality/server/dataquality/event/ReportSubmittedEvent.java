package eu.bbmri_eric.quality.server.dataquality.event;

import org.springframework.context.ApplicationEvent;

public class ReportSubmittedEvent extends ApplicationEvent {
  private final String agentId;
  private final String reportId;

  public ReportSubmittedEvent(Object source, String agentId, String reportId) {
    super(source);
    this.agentId = agentId;
    this.reportId = reportId;
  }

  public String getAgentId() {
    return agentId;
  }

  public String getReportId() {
    return reportId;
  }
}
