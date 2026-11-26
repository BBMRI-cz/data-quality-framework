package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
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

  /**
   * Finds a server with its interactions eagerly loaded, so that detached entities outside a
   * transaction can record new interactions and be merged safely.
   *
   * @param url the URL of the central server
   * @return the server including its interactions, if present
   */
  @EntityGraph(attributePaths = "interactions")
  Optional<Server> findWithInteractionsByUrl(String url);

  boolean existsByUrl(String url);

  List<Server> findAllByStatusIs(ServerConnectionStatus status);

  /**
   * Finds servers whose scheduled automatic registration retry is due.
   *
   * @param cutoff the latest retry date that still counts as due
   * @return the servers with a scheduled retry at or before the cutoff
   */
  List<Server> findAllByNextRegistrationRetryAtLessThanEqual(LocalDateTime cutoff);
}
