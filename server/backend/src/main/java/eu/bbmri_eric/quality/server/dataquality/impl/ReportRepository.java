package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository interface for Report entity. */
@Repository
interface ReportRepository extends JpaRepository<Report, String> {
  Page<Report> findByAgentId(String agentId, Pageable pageable);

  /**
   * Finds a report including all data needed to render it (results, checks, keywords, versions).
   * Used by read-only exports so that lazy keyword collections are fetched with the report instead
   * of one query per result.
   *
   * @param id the report ID
   * @return the report if present
   */
  @EntityGraph(
      attributePaths = {
        "qualityCheckResults",
        "qualityCheckResults.qualityCheck",
        "qualityCheckResults.qualityCheck.keywords",
        "qualityCheckResults.qualityCheck.category",
        "qualityCheckResults.version"
      })
  Optional<Report> findWithDetailsById(String id);
}
