package eu.bbmri_eric.quality.server.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  Optional<User> findBySubjectId(String subjectId);

  Optional<User> findByAgentId(String agentId);
}
