package eu.bbmri_eric.quality.agent.dataquality.event;

public class ReportGeneratedEvent {
  private final Long reportId;

  public ReportGeneratedEvent(Long reportId) {
    this.reportId = reportId;
  }

  public Long getReportId() {
    return reportId;
  }
}
