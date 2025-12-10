package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a quality check definition.
 *
 * <p>Each quality check is uniquely identified by a hash and contains metadata about the check.
 */
@Entity
public class QualityCheck {
  @Id private String hash;
  @NotNull private String name;

  private String description;

  private final LocalDateTime registeredAt = LocalDateTime.now();

  private double warningThreshold = 0.0;

  private double errorThreshold = 0.0;

  /** Default constructor for JPA. */
  protected QualityCheck() {}

  /**
   * Creates a new quality check.
   *
   * @param hash the unique hash identifying this check
   * @param name the name of the check
   * @param description the description of what the check validates
   */
  public QualityCheck(String hash, String name, String description) {
    this.hash = hash;
    this.name = name;
    this.description = description;
  }

  /**
   * Creates a new quality check with thresholds.
   *
   * @param hash the unique hash identifying this check
   * @param name the name of the check
   * @param description the description of what the check validates
   * @param warningThreshold the threshold value for warnings
   * @param errorThreshold the threshold value for errors
   */
  public QualityCheck(
      String hash,
      String name,
      String description,
      double warningThreshold,
      double errorThreshold) {
    this.hash = hash;
    this.name = name;
    this.description = description;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
  }

  /**
   * Gets the unique hash of this quality check.
   *
   * @return the hash
   */
  public String getHash() {
    return hash;
  }

  /**
   * Gets the name of this quality check.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this quality check.
   *
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the description of this quality check.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of this quality check.
   *
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the timestamp when this check was registered.
   *
   * @return the registration timestamp
   */
  public LocalDateTime getRegisteredAt() {
    return registeredAt;
  }

  /**
   * Gets the warning threshold.
   *
   * @return the warning threshold
   */
  public double getWarningThreshold() {
    return warningThreshold;
  }

  /**
   * Sets the warning threshold.
   *
   * @param warningThreshold the warning threshold to set
   */
  public void setWarningThreshold(double warningThreshold) {
    this.warningThreshold = warningThreshold;
  }

  /**
   * Gets the error threshold.
   *
   * @return the error threshold
   */
  public double getErrorThreshold() {
    return errorThreshold;
  }

  /**
   * Sets the error threshold.
   *
   * @param errorThreshold the error threshold to set
   */
  public void setErrorThreshold(double errorThreshold) {
    this.errorThreshold = errorThreshold;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheck that = (QualityCheck) o;
    return Objects.equals(hash, that.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash);
  }
}
