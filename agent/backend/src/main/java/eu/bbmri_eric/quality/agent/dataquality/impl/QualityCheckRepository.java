package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(
    itemResourceRel = "qualityCheck",
    collectionResourceRel = "qualityChecks",
    path = "quality-checks")
interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {}
