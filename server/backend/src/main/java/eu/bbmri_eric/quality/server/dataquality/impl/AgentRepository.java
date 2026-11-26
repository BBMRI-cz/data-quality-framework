package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface AgentRepository extends JpaRepository<Agent, String> {
  Optional<Agent> findFirstByIpAddress(String ipAddress);
}
