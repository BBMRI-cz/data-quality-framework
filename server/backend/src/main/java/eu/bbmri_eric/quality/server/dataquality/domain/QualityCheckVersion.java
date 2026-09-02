package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Entity representing an immutable version of a quality check query.
 *
 * <p>Each version belongs to exactly one {@link QualityCheck} and holds the query text together
 * with a SHA-256 hash of that query. The query and its hash are fixed once the version is created.
 */
@Entity
@Table(name = "quality_check_version")
public class QualityCheckVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "quality_check_id", nullable = false)
  private QualityCheck qualityCheck;

  @Column(name = "version", nullable = false)
  private int version;

  @Column(name = "query", nullable = false, columnDefinition = "TEXT")
  @NotNull
  private String query;

  @Column(name = "hash", nullable = false)
  @NotNull
  private String hash;

  /** Default constructor for JPA. */
  protected QualityCheckVersion() {}

  /**
   * Creates a new quality check version and computes its hash from the query using SHA-256.
   *
   * @param qualityCheck the quality check this version belongs to
   * @param version the version number
   * @param query the query text
   */
  public QualityCheckVersion(QualityCheck qualityCheck, int version, String query) {
    this.qualityCheck = qualityCheck;
    this.version = version;
    this.query = query;
    this.hash = hashOf(query);
  }

  /**
   * Creates a new quality check version with an explicitly provided hash. Intended for backfilling
   * historical versions whose query is no longer available.
   *
   * @param qualityCheck the quality check this version belongs to
   * @param version the version number
   * @param query the query text
   * @param hash the precomputed hash of the query
   */
  public QualityCheckVersion(QualityCheck qualityCheck, int version, String query, String hash) {
    this.qualityCheck = qualityCheck;
    this.version = version;
    this.query = query;
    this.hash = hash;
  }

  /**
   * Computes the SHA-256 hash of the given query, encoded as a lowercase hexadecimal string with
   * zero padding, matching the mechanism used in the agent module.
   *
   * @param query the query text to hash
   * @return the hex-encoded SHA-256 hash
   */
  private static String hashOf(String query) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(query.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }

  /**
   * Gets the numeric id of this quality check version.
   *
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * Gets the quality check this version belongs to.
   *
   * @return the quality check
   */
  public QualityCheck getQualityCheck() {
    return qualityCheck;
  }

  /**
   * Sets the quality check this version belongs to. Only intended to be invoked by {@link
   * QualityCheck} when a version is added.
   *
   * @param qualityCheck the quality check to set
   */
  void setQualityCheck(QualityCheck qualityCheck) {
    this.qualityCheck = qualityCheck;
  }

  /**
   * Gets the version number.
   *
   * @return the version number
   */
  public int getVersion() {
    return version;
  }

  /**
   * Gets the immutable query text.
   *
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * Gets the immutable hash of the query.
   *
   * @return the hash
   */
  public String getHash() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckVersion that = (QualityCheckVersion) o;
    return Objects.equals(qualityCheck, that.qualityCheck) && version == that.version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualityCheck, version);
  }
}
