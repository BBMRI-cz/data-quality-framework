package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.common.CRUDService;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckBulkUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckFilterDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import java.util.List;

/** Service interface for managing quality checks. */
public interface QualityCheckService
    extends CRUDService<QualityCheckDTO, QualityCheckCreateDTO, QualityCheckUpdateDTO, Long> {

  /**
   * Retrieves quality checks with pagination, sorting and category filtering.
   *
   * @param filter the quality check specific filter
   * @return a page response containing filtered quality checks
   */
  PageResponse<QualityCheckDTO> findAll(QualityCheckFilterDTO filter);

  /**
   * Updates multiple quality checks in a single transaction. If any check cannot be found, the
   * whole update is rolled back.
   *
   * @param updateDTOs the bulk update items, each carrying the ID of the check to update
   * @return the updated quality checks
   */
  List<QualityCheckDTO> updateAll(List<QualityCheckBulkUpdateDTO> updateDTOs);
}
