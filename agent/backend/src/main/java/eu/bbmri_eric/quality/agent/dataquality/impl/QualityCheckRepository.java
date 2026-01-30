package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(
    itemResourceRel = "cqlCheck",
    collectionResourceRel = "cqlChecks",
    path = "cql-queries")
interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {}
