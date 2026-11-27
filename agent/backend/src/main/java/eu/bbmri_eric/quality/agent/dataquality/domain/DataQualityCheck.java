package eu.bbmri_eric.quality.agent.dataquality.domain;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;

/**
 * Represents a singular executable query that determines a specific aspect of data quality.
 * Implementations of this interface encapsulate the logic for evaluating data quality against a
 * given data store.
 */
public interface DataQualityCheck {

  /**
   * Upper bound on how much one patient can change a check's raw count, used to calibrate the noise
   * added to that check's result. Defaults to 1 because user-defined checks are expected to count
   * patients, each at most once.
   */
  int DEFAULT_SENSITIVITY = 1;

  /**
   * Executes the data quality check against the specified data store.
   *
   * @param dataStore the data store against which the data quality check should be executed
   * @return the result of the data quality check
   */
  ResultDTO execute(DataStore dataStore);

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
   * Returns the warning threshold used to classify results that are suboptimal, but not critical.
   *
   * @return the warning threshold
   */
  int getWarningThreshold();

  /**
   * Returns the error threshold used to classify results that indicate a serious data quality
   * issue.
   *
   * @return the error threshold
   */
  int getErrorThreshold();

  /**
   * Returns the epsilon budget associated with this check, if any. This value can be used for
   * differential privacy or similar statistical constraints.
   *
   * @return the epsilon budget
   */
  Double getEpsilonBudget();

  /**
   * Returns the unique identifier of this data quality check, if applicable.
   *
   * @return the ID of the check, or {@code null} if not persisted
   */
  Long getId();

  /**
   * Returns the sensitivity assumed when adding noise to this check's result: the upper bound on
   * how much a single patient can change the raw count. Checks whose counting can move the count by
   * more should override this with their own bound.
   *
   * @return the sensitivity of this check's count, {@link #DEFAULT_SENSITIVITY} unless overridden
   */
  default int getSensitivity() {
    return DEFAULT_SENSITIVITY;
  }
}
