package eu.bbmri_eric.quality.agent.dataquality.event;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;

@Getter
public class DataQualityCheckResultEvent {

  private final Long checkId;
  private final String checkName;
  private final int rawValue;
  private final Set<String> patientSet;
  private final String error;
  private final LocalDateTime finishedAt;
  private final int warningThreshold;
  private final int errorThreshold;
  private final float epsilon;
  private final String stratum;

  public DataQualityCheckResultEvent(
      Long checkId,
      String checkName,
      int rawValue,
      Set<String> patientSet,
      String error,
      LocalDateTime finishedAt,
      int warningThreshold,
      int errorThreshold,
      float epsilon,
      String stratum) {
    this.checkId = checkId;
    this.checkName = checkName;
    this.rawValue = rawValue;
    this.patientSet = patientSet;
    this.error = error;
    this.finishedAt = finishedAt;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.epsilon = epsilon;
    this.stratum = stratum;
  }
}
