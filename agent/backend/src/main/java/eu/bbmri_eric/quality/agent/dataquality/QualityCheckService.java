package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.common.CRUDService;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;

/** Service interface for managing quality checks. */
public interface QualityCheckService
    extends CRUDService<QualityCheckDTO, QualityCheckCreateDTO, QualityCheckUpdateDTO, Long> {}
