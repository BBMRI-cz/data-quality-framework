package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity representing a quality check definition.
 *
 * <p>Each quality check is uniquely identified by a numeric id and carries a unique hash as its
 * business key.
 */
@Entity
public class QualityCheck {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "hash", nullable = false, unique = true)
  @NotNull
  private String hash;

  private final Instant registeredAt = Instant.now();
  @NotNull private String name;
  private String description;
  private double warningThreshold = 0.0;

  private double errorThreshold = 0.0;

  @ManyToOne
  @JoinColumn(name = "category_id")
  private Category category;

  @OneToMany(
      mappedBy = "qualityCheckId",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = jakarta.persistence.FetchType.LAZY)
  private final Set<QualityCheckKeyword> keywords = new HashSet<>();

  @OneToMany(
      mappedBy = "qualityCheck",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private final Set<QualityCheckVersion> versions = new HashSet<>();

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

  public QualityCheck(
      String hash,
      String name,
      String description,
      double warningThreshold,
      double errorThreshold,
      Category category) {
    this.hash = hash;
    this.name = name;
    this.description = description;
    this.warningThreshold = warningThreshold;
    this.errorThreshold = errorThreshold;
    this.category = category;
  }

  /**
   * Gets the numeric id of this quality check.
   *
   * @return the id
   */
  public Long getId() {
    return id;
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
  public Instant getRegisteredAt() {
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

  /**
   * Gets the category this quality check belongs to.
   *
   * @return the category, or null if not assigned to any category
   */
  public Category getCategory() {
    return category;
  }

  /**
   * Sets the category this quality check belongs to.
   *
   * @param category the category to set, or null to remove category assignment
   */
  public void setCategory(Category category) {
    this.category = category;
  }

  /**
   * Gets the keywords associated with this quality check.
   *
   * @return the set of keywords (lazy-loaded)
   */
  public Set<QualityCheckKeyword> getKeywords() {
    return keywords;
  }

  /**
   * Sets the keywords for this quality check, replacing all existing keywords.
   *
   * @param newKeywords the new set of keywords to assign
   */
  public void setKeywords(Set<String> newKeywords) {
    keywords.clear();
    for (String keyword : newKeywords) {
      QualityCheckKeyword qualityCheckKeyword = new QualityCheckKeyword(this.id, keyword);
      keywords.add(qualityCheckKeyword);
    }
  }

  /**
   * Gets the versions of this quality check (lazy-loaded).
   *
   * @return the set of versions
   */
  public Set<QualityCheckVersion> getVersions() {
    return versions;
  }

  /**
   * Adds a new version to this quality check, establishing the back-reference from the version.
   *
   * @param version the version to add
   */
  public void addVersion(QualityCheckVersion version) {
    version.setQualityCheck(this);
    versions.add(version);
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
