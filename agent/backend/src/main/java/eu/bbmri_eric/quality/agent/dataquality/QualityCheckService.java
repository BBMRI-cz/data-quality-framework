package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import java.util.List;

/** Service interface for managing quality checks. */
public interface QualityCheckService {

  /**
   * Finds a quality check by its ID.
   *
   * @param id the quality check ID
   * @return the quality check DTO
   */
  QualityCheckDTO findById(Long id);

  /**
   * Finds all quality checks.
   *
   * @return list of all quality check DTOs
   */
  List<QualityCheckDTO> findAll();
}
