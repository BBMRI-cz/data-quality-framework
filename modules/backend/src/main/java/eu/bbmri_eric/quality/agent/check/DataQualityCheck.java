package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;

/**
 * Represents a singular executable query that determines a specific aspect of data quality.
 * Implementations of this interface encapsulate the logic for evaluating data quality
 * against a given FHIR data store.
 */
public interface DataQualityCheck {

  /**
   * Executes the data quality check against the specified FHIR store.
   *
   * @param fhirStore the FHIR store against which the data quality check should be executed
   * @return the result of the data quality check
   */
  Result execute(FHIRStore fhirStore);

  /**
   * Returns the human-readable name of the data quality check.
   *
   * @return the name of the check
   */
  String getName();

  /**
   * Returns a detailed description of what the data quality check evaluates.
   *
   * @return the description of the check
   */
  String getDescription();

  /**
   * Returns the warning threshold used to classify results that are suboptimal,
   * but not critical.
   *
   * @return the warning threshold
   */
  int getWarningThreshold();

  /**
   * Returns the error threshold used to classify results that indicate a serious
   * data quality issue.
   *
   * @return the error threshold
   */
  int getErrorThreshold();

  /**
   * Returns the epsilon budget associated with this check, if any.
   * This value can be used for differential privacy or similar statistical constraints.
   *
   * @return the epsilon budget
   */
  float getEpsilonBudget();

  /**
   * Returns the unique identifier of this data quality check, if applicable.
   *
   * @return the ID of the check, or {@code null} if not persisted
   */
  Long getId();
}
