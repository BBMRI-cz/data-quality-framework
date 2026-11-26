package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface AgentRepository extends JpaRepository<Agent, String> {
  Optional<Agent> findFirstByIpAddress(String ipAddress);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Agent a where a.id = :id")
  Optional<Agent> findByIdForUpdate(@Param("id") String id);
}
