package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReportRepository extends JpaRepository<Report, Long> {
  List<Report> findAllByStatusIs(ReportStatus status);
}
