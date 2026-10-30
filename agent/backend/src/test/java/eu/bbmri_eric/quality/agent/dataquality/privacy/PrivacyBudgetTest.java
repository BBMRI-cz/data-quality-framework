package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class PrivacyBudgetTest {

  @Test
  void checkAndTotalsShares_sumToFullBudget() {
    assertThat(
            PrivacyBudget.CHECK_RESULTS_EPSILON_FRACTION
                + PrivacyBudget.ENTITY_TOTALS_EPSILON_FRACTION)
        .isEqualTo(1.0);
  }

  @Test
  void epsilonPerEntityTotal_isHalfOfTheTotalsShare() {
    assertThat(PrivacyBudget.epsilonPerEntityTotal(3.0)).isCloseTo(0.3, within(1e-12));
    assertThat(PrivacyBudget.epsilonForCheckResults(3.0)).isCloseTo(2.4, within(1e-12));
  }

  @Test
  void refreshThreshold_scalesWithSensitivity() {
    assertThat(PrivacyBudget.refreshThreshold(1)).isEqualTo(5);
    assertThat(PrivacyBudget.refreshThreshold(20)).isEqualTo(100);
  }
}
