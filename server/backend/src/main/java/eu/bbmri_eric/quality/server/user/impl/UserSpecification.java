package eu.bbmri_eric.quality.server.user.impl;

import eu.bbmri_eric.quality.server.user.UserRole;
import eu.bbmri_eric.quality.server.user.domain.User;
import eu.bbmri_eric.quality.server.user.dto.UserFilterDTO;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/** JPA Specifications for querying {@link User} entities. */
class UserSpecification {

  private UserSpecification() {}

  /**
   * Builds a specification combining all filters set on the given {@link UserFilterDTO}.
   *
   * @param filter the filter to translate into a specification; unset fields are ignored
   * @return a specification matching users satisfying every set filter
   */
  static Specification<User> fromFilter(UserFilterDTO filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      String search = filter.getSearch();
      if (search != null && !search.isBlank()) {
        String pattern = "%" + escapeLikePattern(search.toLowerCase(Locale.ROOT)) + "%";
        predicates.add(
            criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern, '\\'),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("subjectId")), pattern, '\\')));
      }

      Set<UserRole> roles = filter.getRoles();
      if (roles != null && !roles.isEmpty()) {
        predicates.add(root.join("roles").in(roles));
        query.distinct(true);
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Escapes SQL LIKE special characters ({@code \}, {@code %}, {@code _}) so that user input is
   * matched literally. The backslash is the escape character used by the LIKE predicates.
   *
   * @param value the unescaped search text
   * @return the escaped text safe for use in a LIKE pattern with {@code \} as escape character
   */
  private static String escapeLikePattern(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
