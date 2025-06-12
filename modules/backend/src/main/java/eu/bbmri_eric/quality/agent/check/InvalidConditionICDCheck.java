package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;

public class InvalidConditionICDCheck implements Check {
  @Override
  public Result execute(FHIRStore fhirStore) {
    return null;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public int getWarningThreshold() {
    return 0;
  }

  @Override
  public int getErrorThreshold() {
    return 0;
  }

  @Override
  public float getEpsilonBudget() {
    return 0;
  }

  @Override
  public Long getId() {
    return 0L;
  }
}
