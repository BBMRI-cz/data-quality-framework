package eu.bbmri_eric.quality.agent.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(itemResourceRel = "report", collectionResourceRel = "reports")
interface ReportRepository extends JpaRepository<Report, Long> {}
