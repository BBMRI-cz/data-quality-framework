package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** JPA Specifications for querying {@link AuditLogEntry} entities. */
class AuditLogSpecification {

  private AuditLogSpecification() {}

  /**
   * Builds a specification combining all filters set on the given {@link AuditLogFilterDTO}.
   *
   * @param filter the filter to translate into a specification; unset fields are ignored
   * @return a specification matching entries satisfying every set filter
   */
  static Specification<AuditLogEntry> fromFilter(AuditLogFilterDTO filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      AuditAction action = filter.getAction();
      if (action != null) {
        predicates.add(criteriaBuilder.equal(root.get("action"), action));
      }

      String actor = filter.getActor();
      if (actor != null && !actor.isBlank()) {
        predicates.add(criteriaBuilder.equal(root.get("actor"), actor));
      }

      LocalDateTime dateFrom = filter.getDateFrom();
      if (dateFrom != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), dateFrom));
      }

      LocalDateTime dateTo = filter.getDateTo();
      if (dateTo != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), dateTo));
      }

      String search = filter.getSearch();
      if (search != null && !search.isBlank()) {
        String pattern = "%" + search.toLowerCase() + "%";
        predicates.add(
            criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("actor")), pattern),
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("action").as(String.class)), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("details")), pattern)));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Builds a specification for {@link org.springframework.boot.actuate.audit.AuditEventRepository#find}
   * query semantics: matches entries by principal/after/type, each ignored when {@code null}.
   */
  static Specification<AuditLogEntry> forAuditQuery(String principal, Instant after, AuditAction action) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (principal != null) {
        predicates.add(criteriaBuilder.equal(root.get("actor"), principal));
      }

      if (after != null) {
        predicates.add(
            criteriaBuilder.greaterThan(
                root.get("timestamp"), LocalDateTime.ofInstant(after, ZoneOffset.UTC)));
      }

      if (action != null) {
        predicates.add(criteriaBuilder.equal(root.get("action"), action));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}