package eu.bbmri_eric.quality.agent.report;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.util.HashSet;
import java.util.Set;

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
  private String stratum;

  @ElementCollection
  @CollectionTable(name = "result_patient_ids", joinColumns = @JoinColumn(name = "result_id"))
  @Column(name = "patient_id")
  private Set<String> patients = new HashSet<>();

  protected Result() {}

  protected Result(
      String checkName,
      Long checkId,
      int rawValue,
      int obfuscatedValue,
      int warningThreshold,
      int errorThreshold,
      float epsilon,
      String error,
      String stratum) {
    this.checkName = checkName;
    this.checkId = checkId;
    this.rawValue = rawValue;
    this.obfuscatedValue = obfuscatedValue;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.epsilon = epsilon;
    setError(error);
    this.stratum = stratum;
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

  public void setPatients(Set<String> patients) {
    this.patients = patients;
  }

  public Set<String> getPatients() {
    return patients;
  }

  public String getStratum() {
    return stratum;
  }
}
