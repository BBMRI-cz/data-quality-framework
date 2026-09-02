package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestDTO;
import java.util.List;

/** Service interface for managing quality check manifests. */
public interface ManifestService {

  /**
   * Creates a new manifest from the given data.
   *
   * @param createDTO the data used to build the manifest
   * @return the created manifest DTO
   */
  ManifestDTO create(ManifestCreateDTO createDTO);

  /**
   * Finds a manifest by its ID.
   *
   * @param id the manifest id
   * @return the manifest DTO
   * @throws EntityNotFoundException if no manifest with the given id exists
   */
  ManifestDTO findById(Long id);

  /**
   * Finds all manifests.
   *
   * @return list of all manifest DTOs
   */
  List<ManifestDTO> findAll();
}
