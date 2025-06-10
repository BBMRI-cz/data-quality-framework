package eu.bbmri_eric.quality.agent.events;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationEvent;

public class CheckResultEvent extends ApplicationEvent {

  private final Long checkId;
  private final String checkName;
  private final int rawValue;
  private final String error;
  private final LocalDateTime finishedAt;
  private final int warningThreshold;
  private final int errorThreshold;

  public CheckResultEvent(
      Object source,
      Long checkId,
      String checkName,
      int rawValue,
      String error,
      LocalDateTime finishedAt,
      int warningThreshold,
      int errorThreshold) {
    super(source);
    this.checkId = checkId;
    this.checkName = checkName;
    this.rawValue = rawValue;
    this.error = error;
    this.finishedAt = finishedAt;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
  }

  public Long getCheckId() {
    return checkId;
  }

  public int getRawValue() {
    return rawValue;
  }

  public String getError() {
    return error;
  }

  public LocalDateTime getFinishedAt() {
    return finishedAt;
  }

  public int getWarningThreshold() {
    return warningThreshold;
  }

  public int getErrorThreshold() {
    return errorThreshold;
  }

  public String getCheckName() {
    return checkName;
  }
}
