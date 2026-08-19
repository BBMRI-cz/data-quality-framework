package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import java.util.List;
import java.util.Set;

/** Service interface for managing quality checks. */
public interface QualityCheckService {

  /**
   * Creates a new quality check.
   *
   * @param createDTO the create data
   * @return the created quality check DTO
   */
  QualityCheckDTO create(QualityCheckCreateDTO createDTO);

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
   * Sets the keywords for a quality check, replacing all existing keywords.
   *
   * @param id the quality check ID (hash)
   * @param keywords the new set of keywords to assign
   * @return the updated quality check DTO
   * @throws eu.bbmri_eric.quality.server.common.EntityNotFoundException if the keyword is not
   *     associated with the quality check
   */
  QualityCheckDTO setKeywords(String id, Set<String> keywords);
}
