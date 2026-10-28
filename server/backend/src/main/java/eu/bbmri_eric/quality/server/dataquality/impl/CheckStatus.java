package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import java.math.BigDecimal;

/**
 * Status of a quality check result as derived from the check thresholds.
 *
 * <p>Shared by all result presentations (PDF summary, DQV export) so the classification rule is
 * applied identically everywhere. The comparison is strict {@code >} and performed on the exact
 * decimal percentage to avoid binary floating point artifacts (e.g. {@code 0.07 * 100} evaluating
 * to {@code 7.000000000000001}). Non-finite results are FAILED (the same as {@code null} results);
 * non-finite thresholds never trigger a status downgrade.
 */
enum CheckStatus {
  PASSED,
  WARNING,
  FAILED;

  /**
   * Computes the status of a result.
   *
   * @param check the quality check carrying the thresholds
   * @param result the differentially private ratio in [0, 1], or {@code null} when the check was
   *     not computed
   * @return the status; {@code null} and non-finite results are FAILED, the same as for the PDF
   *     summary. Non-finite thresholds are treated as if no comparison applies.
   */
  static CheckStatus of(QualityCheck check, Double result) {
    if (result == null || !Double.isFinite(result)) {
      return FAILED;
    }
    if (greaterThan(result, check.getErrorThreshold())) {
      return FAILED;
    }
    if (greaterThan(result, check.getWarningThreshold())) {
      return WARNING;
    }
    return PASSED;
  }

  private static boolean greaterThan(double value, double threshold) {
    if (!Double.isFinite(value) || !Double.isFinite(threshold)) {
      return false;
    }
    BigDecimal percent = BigDecimal.valueOf(value).movePointRight(2);
    return percent.compareTo(BigDecimal.valueOf(threshold)) > 0;
  }
}
