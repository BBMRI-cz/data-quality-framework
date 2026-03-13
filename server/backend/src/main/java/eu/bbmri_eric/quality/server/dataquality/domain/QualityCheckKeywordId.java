package eu.bbmri_eric.quality.server.dataquality.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite ID class for QualityCheckKeyword entity.
 *
 * <p>Represents the composite primary key consisting of quality check hash and keyword.
 */
class QualityCheckKeywordId implements Serializable {

  private String qualityCheckHash;
  private String keyword;

  /** Default constructor for JPA. */
  protected QualityCheckKeywordId() {}

  /**
   * Creates a new composite ID.
   *
   * @param qualityCheckHash the quality check hash
   * @param keyword the keyword
   */
  public QualityCheckKeywordId(String qualityCheckHash, String keyword) {
    this.qualityCheckHash = qualityCheckHash;
    this.keyword = keyword;
  }

  public String getQualityCheckHash() {
    return qualityCheckHash;
  }

  public void setQualityCheckHash(String qualityCheckHash) {
    this.qualityCheckHash = qualityCheckHash;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckKeywordId that = (QualityCheckKeywordId) o;
    return Objects.equals(qualityCheckHash, that.qualityCheckHash)
        && Objects.equals(keyword, that.keyword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualityCheckHash, keyword);
  }
}
