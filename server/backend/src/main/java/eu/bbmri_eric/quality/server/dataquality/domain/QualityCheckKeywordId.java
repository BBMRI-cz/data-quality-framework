package eu.bbmri_eric.quality.server.dataquality.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite ID class for QualityCheckKeyword entity.
 *
 * <p>Represents the composite primary key consisting of quality check id and keyword.
 */
class QualityCheckKeywordId implements Serializable {

  private Long qualityCheckId;
  private String keyword;

  /** Default constructor for JPA. */
  protected QualityCheckKeywordId() {}

  /**
   * Creates a new composite ID.
   *
   * @param qualityCheckId the quality check id
   * @param keyword the keyword
   */
  public QualityCheckKeywordId(Long qualityCheckId, String keyword) {
    this.qualityCheckId = qualityCheckId;
    this.keyword = keyword;
  }

  public Long getQualityCheckId() {
    return qualityCheckId;
  }

  public void setQualityCheckId(Long qualityCheckId) {
    this.qualityCheckId = qualityCheckId;
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
    return Objects.equals(qualityCheckId, that.qualityCheckId)
        && Objects.equals(keyword, that.keyword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualityCheckId, keyword);
  }
}
