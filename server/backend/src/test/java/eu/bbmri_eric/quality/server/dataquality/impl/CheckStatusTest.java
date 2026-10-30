package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import org.junit.jupiter.api.Test;

class CheckStatusTest {

  @Test
  void of_shouldReturnFailedForNullResult() {
    assertEquals(CheckStatus.FAILED, CheckStatus.of(check(1.0, 5.0), null));
  }

  @Test
  void of_shouldReturnFailedForNonFiniteResults() {
    QualityCheck check = check(1.0, 5.0);
    assertEquals(CheckStatus.FAILED, CheckStatus.of(check, Double.NaN));
    assertEquals(CheckStatus.FAILED, CheckStatus.of(check, Double.POSITIVE_INFINITY));
    assertEquals(CheckStatus.FAILED, CheckStatus.of(check, Double.NEGATIVE_INFINITY));
  }

  @Test
  void of_shouldReturnPassedBelowWarningThreshold() {
    assertEquals(CheckStatus.PASSED, CheckStatus.of(check(5.0, 10.0), 0.05));
  }

  @Test
  void of_shouldReturnWarningAboveWarningThreshold() {
    assertEquals(CheckStatus.WARNING, CheckStatus.of(check(1.0, 10.0), 0.05));
  }

  @Test
  void of_shouldReturnFailedAboveErrorThreshold() {
    assertEquals(CheckStatus.FAILED, CheckStatus.of(check(1.0, 2.0), 0.03));
  }

  @Test
  void of_shouldNotDowngradeAtExactThreshold() {
    // 0.07 * 100 is not exactly 7.0 in binary floating point; the rule must stay exact-decimal.
    assertEquals(CheckStatus.PASSED, CheckStatus.of(check(7.0, 10.0), 0.07));
    assertEquals(CheckStatus.WARNING, CheckStatus.of(check(5.0, 7.0), 0.07));
  }

  @Test
  void of_shouldNotDowngradeForNonFiniteThresholds() {
    assertEquals(CheckStatus.PASSED, CheckStatus.of(check(Double.NaN, Double.NaN), 0.99));
    assertEquals(
        CheckStatus.PASSED, CheckStatus.of(check(Double.NEGATIVE_INFINITY, Double.NaN), 0.99));
  }

  private static QualityCheck check(double warningThreshold, double errorThreshold) {
    return new QualityCheck("check", "", warningThreshold, errorThreshold);
  }
}
