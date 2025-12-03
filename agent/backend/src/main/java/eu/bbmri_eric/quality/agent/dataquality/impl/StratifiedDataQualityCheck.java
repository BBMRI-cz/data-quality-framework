package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.FHIRStore;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.Map;

interface StratifiedDataQualityCheck extends DataQualityCheck {
  Map<String, ResultDTO> executeWithStratification(FHIRStore fhirStore);
}
