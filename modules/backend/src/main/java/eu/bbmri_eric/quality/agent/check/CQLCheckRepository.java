package eu.bbmri_eric.quality.agent.check;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(itemResourceRel = "cqlCheck", collectionResourceRel = "cqlChecks", path = "cql-queries")
interface CQLCheckRepository extends JpaRepository<CQLQuery, Long> {}
