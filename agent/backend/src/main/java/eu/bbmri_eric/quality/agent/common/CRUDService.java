package eu.bbmri_eric.quality.agent.common;

import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import java.util.List;

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
  T findById(ID id);

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
   * @return a page response containing entities and pagination metadata
   * @throws IllegalArgumentException if the filter is null or contains invalid data
   */
  PageResponse<T> findAll(FilterDTO filter);

  /**
   * Updates an existing entity.
   *
   * @param id the identifier of the entity to update
   * @param updateDTO the data transfer object containing the updated entity data
   * @return the updated entity as a DTO
   * @throws IllegalArgumentException if the id or updateDTO is null or contains invalid data
   * @throws eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException if the entity with
   *     the given id does not exist
   */
  T update(ID id, UpdateDTO updateDTO);

  /**
   * Counts the total number of entities.
   *
   * @return the total number of entities
   */
  long count();

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
