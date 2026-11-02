package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LowCountSuppressionTest {

  @Test
  void apply_belowThreshold_suppressesToZero() {
    assertThat(LowCountSuppression.apply(49.9, 50)).isEqualTo(0.0);
    assertThat(LowCountSuppression.apply(0.0, 50)).isEqualTo(0.0);
  }

  @Test
  void apply_atOrAboveThreshold_keepsValue() {
    assertThat(LowCountSuppression.apply(50.0, 50)).isEqualTo(50.0);
    assertThat(LowCountSuppression.apply(1000.0, 50)).isEqualTo(1000.0);
  }

  @Test
  void apply_withNegativeThreshold_throws() {
    assertThatThrownBy(() -> LowCountSuppression.apply(100.0, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Threshold must be non-negative");
  }
}
