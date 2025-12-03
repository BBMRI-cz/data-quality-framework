package eu.bbmri_eric.quality.agent.common;

import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD service interface providing standard Create, Read, Update, and Delete operations.
 *
 * @param <T> the type of entity this service manages (typically a DTO)
 * @param <CreateDTO> the type used for creating entities
 * @param <UpdateDTO> the type used for updating entities
 * @param <ID> the type of the entity's identifier
 */
public interface CRUDService<T, CreateDTO, UpdateDTO, ID> {

  /**
   * Creates a new entity.
   *
   * @param createDTO the data transfer object containing the entity data to create
   * @return the created entity as a DTO
   * @throws IllegalArgumentException if the createDTO is null or contains invalid data
   * @throws eu.bbmri_eric.quality.agent.common.exception.EntityAlreadyExistsException if an entity
   *     with the same unique identifier already exists
   */
  T create(CreateDTO createDTO);

  /**
   * Retrieves an entity by its identifier.
   *
   * @param id the identifier of the entity to retrieve
   * @return an Optional containing the entity if found, or empty if not found
   * @throws IllegalArgumentException if the id is null
   */
  Optional<T> findById(ID id);

  /**
   * Retrieves all entities.
   *
   * @return a list of all entities as DTOs
   */
  List<T> findAll();

  /**
   * Retrieves entities with pagination and sorting.
   *
   * @param filter the filter containing pagination and sorting parameters
   * @return a list of entities as DTOs matching the filter criteria
   * @throws IllegalArgumentException if the filter is null or contains invalid data
   */
  List<T> findAll(FilterDTO filter);

  T update(ID id, UpdateDTO updateDTO);

  /**
   * Deletes an entity by its identifier.
   *
   * @param id the identifier of the entity to delete
   * @throws IllegalArgumentException if the id is null
   * @throws eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException if the entity with
   *     the given id does not exist
   */
  void delete(ID id);

  /**
   * Checks whether an entity with the given identifier exists.
   *
   * @param id the identifier to check
   * @return true if an entity with the given id exists, false otherwise
   * @throws IllegalArgumentException if the id is null
   */
  boolean exists(ID id);
}
