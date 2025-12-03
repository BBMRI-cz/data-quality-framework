package eu.bbmri_eric.quality.agent.user.impl;

import eu.bbmri_eric.quality.agent.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
}
