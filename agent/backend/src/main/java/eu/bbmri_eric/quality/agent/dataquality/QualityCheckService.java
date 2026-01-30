package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import java.util.List;

/** Service interface for managing CQL queries. */
public interface QualityCheckService {

  /**
   * Finds a CQL query by its ID.
   *
   * @param id the CQL query ID
   * @return the CQL query DTO
   */
  QualityCheckDTO findById(Long id);

  /**
   * Finds all CQL queries.
   *
   * @return list of all CQL query DTOs
   */
  List<QualityCheckDTO> findAll();
}
