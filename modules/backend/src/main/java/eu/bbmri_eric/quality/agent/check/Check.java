package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;

interface Check {
  Result execute(FHIRStore fhirStore);

  String getName();

  String getDescription();
}
