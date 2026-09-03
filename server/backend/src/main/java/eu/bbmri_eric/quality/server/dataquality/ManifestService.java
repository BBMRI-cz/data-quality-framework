package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestVersionCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestVersionDTO;
import java.util.List;

/** Service interface for managing quality check manifests. */
public interface ManifestService {

  /**
   * Creates a new manifest with the given metadata.
   *
   * @param createDTO the manifest metadata
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

  /**
   * Publishes a new signed version of a manifest. When no explicit version number is provided, the
   * next sequential version is assigned.
   *
   * @param id the manifest id
   * @param createDTO the version data (hashes, optional version number)
   * @return the created version DTO
   * @throws EntityNotFoundException if no manifest with the given id exists
   */
  ManifestVersionDTO createVersion(Long id, ManifestVersionCreateDTO createDTO);

  /**
   * Finds all versions of a manifest.
   *
   * @param id the manifest id
   * @return list of version DTOs
   * @throws EntityNotFoundException if no manifest with the given id exists
   */
  List<ManifestVersionDTO> findVersions(Long id);
}
