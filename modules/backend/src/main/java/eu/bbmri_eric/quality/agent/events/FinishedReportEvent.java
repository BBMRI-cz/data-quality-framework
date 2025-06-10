package eu.bbmri_eric.quality.agent.events;

import org.springframework.context.ApplicationEvent;

public class FinishedReportEvent extends ApplicationEvent {
    private Long reportId;
    public FinishedReportEvent(Object source) {
        super(source);
    }

    public FinishedReportEvent(Object source, Long reportId) {
        super(source);
        this.reportId = reportId;
    }

    public Long getReportId() {
        return reportId;
    }
}
