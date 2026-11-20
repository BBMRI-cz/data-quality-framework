package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface AgentRepository extends JpaRepository<Agent, String> {
  List<Agent> findByIpAddressIn(Collection<String> ipAddresses);
}
