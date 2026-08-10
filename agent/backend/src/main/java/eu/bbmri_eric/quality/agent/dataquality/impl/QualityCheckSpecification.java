package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.Category;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/** JPA Specifications for querying {@link QualityCheck} entities. */
class QualityCheckSpecification {

  private QualityCheckSpecification() {}

  /**
   * Builds a specification that filters quality checks by category name.
   *
   * <ul>
   *   <li>{@code null} category name matches all quality checks (no filtering).
   *   <li>Blank category name matches only quality checks without a category.
   *   <li>Non-blank category name matches quality checks whose category name equals the given
   *       value.
   * </ul>
   *
   * @param categoryName the category name filter; may be {@code null}
   * @return a specification implementing the described filter semantics
   */
  static Specification<QualityCheck> withCategoryName(String categoryName) {
    if (categoryName == null) {
      return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }
    if (categoryName.isBlank()) {
      return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("category"));
    }
    return (root, query, criteriaBuilder) -> {
      Join<QualityCheck, Category> categoryJoin = root.join("category", JoinType.INNER);
      return criteriaBuilder.equal(categoryJoin.get("name"), categoryName);
    };
  }
}
