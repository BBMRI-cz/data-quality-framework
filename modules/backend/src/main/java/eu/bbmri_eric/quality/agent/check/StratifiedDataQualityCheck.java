package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.util.Map;

interface StratifiedDataQualityCheck extends DataQualityCheck {
  Map<String, Result> executeWithStratification(FHIRStore fhirStore);
}
