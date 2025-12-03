package eu.bbmri_eric.quality.agent.dataquality.event;

public class NewReportEvent {
  private Long reportId;

  public NewReportEvent(Long reportId) {
    this.reportId = reportId;
  }

  public Long getReportId() {
    return reportId;
  }
}
