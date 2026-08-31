package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
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
  QualityCheckDTO findById(Long id);

  /**
   * Finds all quality checks.
   *
   * @return list of all quality check DTOs
   */
  List<QualityCheckDTO> findAll();

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
}
