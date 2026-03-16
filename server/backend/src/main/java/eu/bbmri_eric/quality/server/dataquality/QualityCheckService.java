package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import java.util.List;

/** Service interface for managing quality checks. */
public interface QualityCheckService {

  /**
   * Finds a quality check by its ID (hash).
   *
   * @param id the quality check ID (hash)
   * @return the quality check DTO
   */
  QualityCheckDTO findById(String id);

  /**
   * Finds all quality checks.
   *
   * @return list of all quality check DTOs
   */
  List<QualityCheckDTO> findAll();

  /**
   * Updates an existing quality check.
   *
   * @param id the quality check ID (hash)
   * @param updateDTO the update data
   * @return the updated quality check DTO
   */
  QualityCheckDTO update(String id, QualityCheckUpdateDTO updateDTO);

  /**
   * Adds a keyword to a quality check.
   *
   * @param id the quality check ID (hash)
   * @param keyword the keyword to add (max 250 characters)
   * @return the updated quality check DTO
   */
  QualityCheckDTO addKeyword(String id, String keyword);

  /**
   * Removes a keyword from a quality check.
   *
   * @param id the quality check ID (hash)
   * @param keyword the keyword to remove
   * @return the updated quality check DTO
   * @throws eu.bbmri_eric.quality.server.common.EntityNotFoundException if the keyword is not
   *     associated with the quality check
   */
  QualityCheckDTO removeKeyword(String id, String keyword) throws EntityNotFoundException;
}
