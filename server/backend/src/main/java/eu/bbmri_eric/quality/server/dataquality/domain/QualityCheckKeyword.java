package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Entity representing a keyword associated with a quality check.
 *
 * <p>Each keyword is uniquely identified by a combination of the quality check hash and the keyword
 * text.
 */
@Entity
@Table(name = "quality_check_keyword")
@IdClass(QualityCheckKeywordId.class)
public class QualityCheckKeyword {

  @Id
  @Column(name = "quality_check_hash")
  @NotNull
  private String qualityCheckHash;

  @Id
  @Column(name = "keyword", length = 250)
  @NotNull
  private String keyword;

  /** Default constructor for JPA. */
  protected QualityCheckKeyword() {}

  /**
   * Creates a new quality check keyword.
   *
   * @param qualityCheckHash the quality check hash
   * @param keyword the keyword text (max 250 characters)
   */
  public QualityCheckKeyword(String qualityCheckHash, String keyword) {
    this.qualityCheckHash = qualityCheckHash;
    this.keyword = keyword;
  }

  /**
   * Gets the quality check hash.
   *
   * @return the quality check hash
   */
  public String getQualityCheckHash() {
    return qualityCheckHash;
  }

  /**
   * Sets the quality check hash.
   *
   * @param qualityCheckHash the quality check hash to set
   */
  public void setQualityCheckHash(String qualityCheckHash) {
    this.qualityCheckHash = qualityCheckHash;
  }

  /**
   * Gets the keyword text.
   *
   * @return the keyword
   */
  public String getKeyword() {
    return keyword;
  }

  /**
   * Sets the keyword text.
   *
   * @param keyword the keyword to set
   */
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckKeyword that = (QualityCheckKeyword) o;
    return Objects.equals(qualityCheckHash, that.qualityCheckHash)
        && Objects.equals(keyword, that.keyword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualityCheckHash, keyword);
  }
}
