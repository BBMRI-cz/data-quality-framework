package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDetailedDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionDTO;
import java.util.List;
import java.util.Set;

/** Service interface for managing quality checks. */
public interface QualityCheckService {

  /**
   * Finds a quality check by its ID.
   *
   * @param id the quality check id
   * @return the quality check DTO
   */
  QualityCheckDetailedDTO findById(Long id);

  /**
   * Finds all quality checks, including their versions.
   *
   * @return list of all quality check detailed DTOs
   */
  List<QualityCheckDetailedDTO> findAll();

  /**
   * Updates an existing quality check.
   *
   * @param id the quality check id
   * @param updateDTO the update data
   * @return the updated quality check DTO
   */
  QualityCheckDTO update(Long id, QualityCheckUpdateDTO updateDTO);

  /**
   * Sets the keywords for a quality check, replacing all existing keywords.
   *
   * @param id the quality check id
   * @param keywords the new set of keywords to assign
   * @return the updated quality check DTO
   * @throws eu.bbmri_eric.quality.server.common.EntityNotFoundException if the keyword is not
   *     associated with the quality check
   */
  QualityCheckDTO setKeywords(Long id, Set<String> keywords);

  /**
   * Creates a new version of a quality check. The version's hash is computed from the query using
   * SHA-256. When no version number is supplied, the next sequential version is assigned.
   *
   * @param id the quality check id
   * @param createDTO the version data (query, optional version)
   * @return the created version DTO
   * @throws eu.bbmri_eric.quality.server.common.EntityNotFoundException if the quality check is not
   *     found
   */
  QualityCheckVersionDTO createVersion(Long id, QualityCheckVersionCreateDTO createDTO);

  /**
   * Finds all versions of a quality check.
   *
   * @param id the quality check id
   * @return list of version DTOs
   * @throws eu.bbmri_eric.quality.server.common.EntityNotFoundException if the quality check is not
   *     found
   */
  List<QualityCheckVersionDTO> findVersions(Long id);
}
