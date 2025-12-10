package eu.bbmri_eric.quality.server.dataquality;

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
}
