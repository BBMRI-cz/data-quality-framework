package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.dto.CQLQueryDTO;
import java.util.List;

/** Service interface for managing CQL queries. */
public interface CQLQueryService {

  /**
   * Finds a CQL query by its ID.
   *
   * @param id the CQL query ID
   * @return the CQL query DTO
   */
  CQLQueryDTO findById(Long id);

  /**
   * Finds all CQL queries.
   *
   * @return list of all CQL query DTOs
   */
  List<CQLQueryDTO> findAll();
}
