package eu.bbmri_eric.quality.agent.dataquality.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single data quality check result.
 *
 * <p>This entity captures the outcome of executing a data quality check, including the raw and
 * obfuscated values, threshold comparisons, and any errors encountered. Results can be stratified
 * and include a collection of affected patient identifiers.
 */
@Entity
@Getter
@Setter
public class Result {
  private static final int MAX_ERROR_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String checkName = "";
  private Long checkId;
  private int rawValue;
  private Double obfuscatedValue;
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

  public Result(
      String checkName,
      Long checkId,
      int rawValue,
      double obfuscatedValue,
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

  public void setError(String error) {
    if (error != null && error.length() > MAX_ERROR_LENGTH) {
      this.error = error.substring(0, MAX_ERROR_LENGTH - 20) + "...[truncated]";
    } else {
      this.error = error;
    }
  }
}
