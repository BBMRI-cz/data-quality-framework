package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository interface for Report entity. */
@Repository
interface ReportRepository extends JpaRepository<Report, String> {
  Page<Report> findByAgentId(String agentId, Pageable pageable);
}
