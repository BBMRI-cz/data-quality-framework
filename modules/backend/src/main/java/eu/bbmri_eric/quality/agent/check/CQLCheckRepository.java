package eu.bbmri_eric.quality.agent.check;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface CQLCheckRepository extends JpaRepository<CQLCheck, Long> {}
