package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.Category;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** JPA Specifications for querying {@link QualityCheck} entities. */
class QualityCheckSpecification {

  private QualityCheckSpecification() {}

  /**
   * Builds a specification that filters quality checks by category name.
   *
   * <ul>
   *   <li>{@code null} matches all quality checks (no filtering).
   *   <li>Blank category names match quality checks without a category. An empty list is what a
   *       single blank request parameter binds to, so it selects uncategorized checks as well.
   *   <li>Non-blank category names match quality checks whose category name is one of them.
   *   <li>Names are combined with OR, also when combined with a blank one.
   * </ul>
   *
   * @param categoryNames the category name filters; may be {@code null} and may contain blank
   *     values
   * @return a specification implementing the described filter semantics
   */
  static Specification<QualityCheck> withCategoryNames(List<String> categoryNames) {
    if (categoryNames == null) {
      return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    List<String> names = categoryNames.stream().filter(QualityCheckSpecification::isNamed).toList();
    boolean matchesUncategorized = names.isEmpty() || names.size() < categoryNames.size();

    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (matchesUncategorized) {
        predicates.add(criteriaBuilder.isNull(root.get("category")));
      }
      if (!names.isEmpty()) {
        Join<QualityCheck, Category> categoryJoin = root.join("category", JoinType.LEFT);
        predicates.add(categoryJoin.get("name").in(names));
      }
      return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
    };
  }

  private static boolean isNamed(String categoryName) {
    return categoryName != null && !categoryName.isBlank();
  }
}
