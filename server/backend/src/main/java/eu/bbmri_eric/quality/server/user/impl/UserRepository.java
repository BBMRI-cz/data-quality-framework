package eu.bbmri_eric.quality.server.user.impl;

import eu.bbmri_eric.quality.server.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  Optional<User> findByUsername(String username);

  Optional<User> findBySubjectId(String subjectId);

  Optional<User> findByAgentId(String agentId);
}
