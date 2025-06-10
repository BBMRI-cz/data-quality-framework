package eu.bbmri_eric.quality.agent.report;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(itemResourceRel = "report", collectionResourceRel = "reports")
interface ReportRepository extends JpaRepository<Report, Long> {
  List<Report> findAllByStatusIs(Status status);
}
