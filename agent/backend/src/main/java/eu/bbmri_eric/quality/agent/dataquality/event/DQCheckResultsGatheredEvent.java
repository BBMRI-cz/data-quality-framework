package eu.bbmri_eric.quality.agent.dataquality.event;

import lombok.Getter;

@Getter
public class DQCheckResultsGatheredEvent {
  private final Long reportId;

  public DQCheckResultsGatheredEvent(Long reportId) {
    this.reportId = reportId;
  }
}
