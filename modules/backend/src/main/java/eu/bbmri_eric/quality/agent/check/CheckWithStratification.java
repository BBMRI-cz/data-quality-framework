package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.util.Map;

public interface CheckWithStratification extends Check {
  Map<String, Result> executeWithStratification(FHIRStore fhirStore);
}
