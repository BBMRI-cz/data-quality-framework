package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.Map;

// TODO: remove all support for this
interface StratifiedDataQualityCheck extends DataQualityCheck {
  Map<String, ResultDTO> executeWithStratification(FHIRServer fhirStore);
}
