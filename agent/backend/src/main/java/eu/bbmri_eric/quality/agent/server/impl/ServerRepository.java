package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link eu.bbmri_eric.quality.agent.server.domain.Server} entity
 * persistence operations.
 *
 * <p>Provides CRUD operations for managing central server configurations in the database.
 */
@Repository
interface ServerRepository extends CrudRepository<Server, String> {
  Optional<Server> findByUrl(String url);

  List<Server> findAllByStatusIs(ServerConnectionStatus status);
}
