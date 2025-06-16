package eu.bbmri_eric.quality.agent.report;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
class Result {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private static final int MAX_ERROR_LENGTH = 255;

  private String checkName;
  private Long checkId;
  private int rawValue;
  private int obfuscatedValue;
  private int warningThreshold;
  private int errorThreshold;
  private float epsilon;
  private String error;

  protected Result() {}

  protected Result(
      String checkName,
      Long checkId,
      int rawValue,
      int obfuscatedValue,
      int warningThreshold,
      int errorThreshold,
      float epsilon,
      String error) {
    this.checkName = checkName;
    this.checkId = checkId;
    this.rawValue = rawValue;
    this.obfuscatedValue = obfuscatedValue;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.epsilon = epsilon;
    setError(error);
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public String getCheckName() {
    return checkName;
  }

  public Long getCheckId() {
    return checkId;
  }

  public int getRawValue() {
    return rawValue;
  }

  public int getObfuscatedValue() {
    return obfuscatedValue;
  }

  public int getWarningThreshold() {
    return warningThreshold;
  }

  public void setWarningThreshold(int warningThreshold) {
    this.warningThreshold = warningThreshold;
  }

  public int getErrorThreshold() {
    return errorThreshold;
  }

  public void setErrorThreshold(int errorThreshold) {
    this.errorThreshold = errorThreshold;
  }

  public float getEpsilon() {
    return epsilon;
  }

  public void setEpsilon(float epsilon) {
    this.epsilon = epsilon;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    if (error != null && error.length() > MAX_ERROR_LENGTH) {
      this.error = error.substring(0, MAX_ERROR_LENGTH - 20) + "...[truncated]";
    } else {
      this.error = error;
    }
  }
}
