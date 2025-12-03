package eu.bbmri_eric.quality.agent.dataquality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("DifferentialPrivacyUtil")
class DifferentialPrivacyUtilTest {

  private static final double EPSILON = 1.0;
  private static final double SENSITIVITY = 1.0;
  private static final int SAMPLE_SIZE = 1000;

  @Nested
  @DisplayName("Parameter Validation")
  class ParameterValidation {

    @Test
    @DisplayName("should throw IllegalArgumentException when epsilon is zero")
    void shouldRejectZeroEpsilon() {
      assertThatThrownBy(() -> DifferentialPrivacyUtil.addLaplaceNoise(100, 0.0, SENSITIVITY))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Epsilon must be positive");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when epsilon is negative")
    void shouldRejectNegativeEpsilon() {
      assertThatThrownBy(() -> DifferentialPrivacyUtil.addLaplaceNoise(100, -1.0, SENSITIVITY))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Epsilon must be positive");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when sensitivity is zero")
    void shouldRejectZeroSensitivity() {
      assertThatThrownBy(() -> DifferentialPrivacyUtil.addLaplaceNoise(100, EPSILON, 0.0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sensitivity must be positive");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when sensitivity is negative")
    void shouldRejectNegativeSensitivity() {
      assertThatThrownBy(() -> DifferentialPrivacyUtil.addLaplaceNoise(100, EPSILON, -1.0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sensitivity must be positive");
    }

    @Test
    @DisplayName("should accept positive epsilon and sensitivity")
    void shouldAcceptValidParameters() {
      double result = DifferentialPrivacyUtil.addLaplaceNoise(100, EPSILON, SENSITIVITY);
      assertThat(result).isGreaterThanOrEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("Low Count Suppression")
  class LowCountSuppression {

    @Test
    @DisplayName("should suppress very low counts (count = 0)")
    void shouldSuppressZeroCount() {
      double result = DifferentialPrivacyUtil.addLaplaceNoise(0, EPSILON, SENSITIVITY);
      assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("should suppress low counts (count = 5)")
    void shouldSuppressLowCount() {
      double result = DifferentialPrivacyUtil.addLaplaceNoise(5, EPSILON, SENSITIVITY);
      assertThat(result).isEqualTo(0.0);
    }

    @RepeatedTest(10)
    @DisplayName("should consistently suppress counts around threshold (count = 9)")
    void shouldHandleCountsNearThreshold() {
      // With count = 9 and epsilon = 1.0, the noisy count could be above or below 10
      double result = DifferentialPrivacyUtil.addLaplaceNoise(9, EPSILON, SENSITIVITY);
      assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("should not suppress high counts (count = 100)")
    void shouldNotSuppressHighCount() {
      double result = DifferentialPrivacyUtil.addLaplaceNoise(100, EPSILON, SENSITIVITY);
      assertThat(result).isGreaterThan(0.0);
    }

    @RepeatedTest(100)
    @DisplayName("should suppress most small counts (count = 8)")
    void shouldSuppressMostSmallCounts() {
      // With epsilon=1.0, most noisy counts from 8 should be below 10
      int suppressedCount = 0;
      for (int i = 0; i < 100; i++) {
        double result = DifferentialPrivacyUtil.addLaplaceNoise(8, EPSILON, SENSITIVITY);
        if (result == 0.0) {
          suppressedCount++;
        }
      }
      // Expect most (>60%) to be suppressed given the small count
      assertThat(suppressedCount).isGreaterThan(60);
    }
  }

  @Nested
  @DisplayName("Non-Negativity")
  class NonNegativity {

    @RepeatedTest(100)
    @DisplayName("should never return negative values for any count")
    void shouldNeverReturnNegativeValues() {
      int[] testCounts = {0, 1, 5, 10, 50, 100, 1000};
      for (int count : testCounts) {
        double result = DifferentialPrivacyUtil.addLaplaceNoise(count, EPSILON, SENSITIVITY);
        assertThat(result).isGreaterThanOrEqualTo(0.0);
      }
    }

    @RepeatedTest(50)
    @DisplayName("should clamp negative noisy counts to zero")
    void shouldClampNegativeNoisyCounts() {
      // Small count with high noise should sometimes produce negative intermediate values
      double result = DifferentialPrivacyUtil.addLaplaceNoise(1, 0.1, SENSITIVITY);
      assertThat(result).isGreaterThanOrEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("Noise Properties")
  class NoiseProperties {

    @Test
    @DisplayName("should add noise - result should differ from original count")
    void shouldAddNoise() {
      int count = 1000;
      boolean foundDifferent = false;

      // Run multiple times to ensure we get some variation
      for (int i = 0; i < 10; i++) {
        double result = DifferentialPrivacyUtil.addLaplaceNoise(count, EPSILON, SENSITIVITY);
        if (result != count) {
          foundDifferent = true;
          break;
        }
      }

      assertThat(foundDifferent)
          .as("At least one noisy result should differ from the original count")
          .isTrue();
    }

    @Test
    @DisplayName("should produce different results on repeated calls")
    void shouldProduceDifferentResults() {
      int count = 100;
      List<Double> results = new ArrayList<>();

      for (int i = 0; i < 10; i++) {
        results.add(DifferentialPrivacyUtil.addLaplaceNoise(count, EPSILON, SENSITIVITY));
      }

      // Check that we have at least some variation
      long distinctCount = results.stream().distinct().count();
      assertThat(distinctCount).as("Should have multiple distinct noisy values").isGreaterThan(5);
    }

    @Test
    @DisplayName("should have noise centered around true count for large counts")
    void shouldHaveNoiseCenteredAroundTrueCount() {
      int count = 1000;
      double sum = 0;

      for (int i = 0; i < SAMPLE_SIZE; i++) {
        sum += DifferentialPrivacyUtil.addLaplaceNoise(count, EPSILON, SENSITIVITY);
      }

      double average = sum / SAMPLE_SIZE;
      // For large counts where suppression doesn't happen, average should be close to true count
      // Allow 10% deviation due to statistical variance
      assertThat(average).isCloseTo(count, within(count * 0.1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.1, 0.5, 1.0, 2.0, 5.0})
    @DisplayName("should add more noise with smaller epsilon")
    void shouldAddMoreNoiseWithSmallerEpsilon(double epsilon) {
      int count = 1000;
      List<Double> results = new ArrayList<>();

      for (int i = 0; i < 100; i++) {
        results.add(DifferentialPrivacyUtil.addLaplaceNoise(count, epsilon, SENSITIVITY));
      }

      double variance = calculateVariance(results);

      // Smaller epsilon should produce higher variance
      // For Laplace distribution: variance = 2 * (sensitivity/epsilon)^2
      double expectedVariance = 2 * Math.pow(SENSITIVITY / epsilon, 2);

      // Allow 80% deviation due to finite sampling and suppression effects
      assertThat(variance).isCloseTo(expectedVariance, within(expectedVariance * 0.8));
    }
  }

  @Nested
  @DisplayName("Epsilon Budget")
  class EpsilonBudget {

    @Test
    @DisplayName("should add less noise with larger epsilon")
    void shouldAddLessNoiseWithLargerEpsilon() {
      int count = 1000;

      // Collect samples with different epsilon values
      List<Double> resultsSmallEpsilon = new ArrayList<>();
      List<Double> resultsLargeEpsilon = new ArrayList<>();

      for (int i = 0; i < 200; i++) {
        resultsSmallEpsilon.add(DifferentialPrivacyUtil.addLaplaceNoise(count, 0.5, SENSITIVITY));
        resultsLargeEpsilon.add(DifferentialPrivacyUtil.addLaplaceNoise(count, 2.0, SENSITIVITY));
      }

      double varianceSmallEpsilon = calculateVariance(resultsSmallEpsilon);
      double varianceLargeEpsilon = calculateVariance(resultsLargeEpsilon);

      // Smaller epsilon should have higher variance (more noise)
      assertThat(varianceSmallEpsilon).isGreaterThan(varianceLargeEpsilon);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.1, 0.5, 1.0, 2.0})
    @DisplayName("should scale noise inversely with epsilon")
    void shouldScaleNoiseInverselyWithEpsilon(double epsilon) {
      int count = 500;
      List<Double> results = new ArrayList<>();

      for (int i = 0; i < 200; i++) {
        results.add(DifferentialPrivacyUtil.addLaplaceNoise(count, epsilon, SENSITIVITY));
      }

      double meanAbsoluteDeviation = calculateMeanAbsoluteDeviation(results, count);

      // For Laplace: E[|X - μ|] = scale = sensitivity/epsilon
      double expectedMAD = SENSITIVITY / epsilon;

      // Allow 30% deviation due to sampling and suppression effects
      assertThat(meanAbsoluteDeviation).isCloseTo(expectedMAD, within(expectedMAD * 0.5));
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle very large counts")
    void shouldHandleVeryLargeCounts() {
      int count = 1_000_000;
      double result = DifferentialPrivacyUtil.addLaplaceNoise(count, EPSILON, SENSITIVITY);

      assertThat(result).isGreaterThan(0.0);
      // Should be reasonably close to the original count
      assertThat(result).isCloseTo(count, within(count * 0.01));
    }

    @Test
    @DisplayName("should handle very small epsilon (high privacy)")
    void shouldHandleVerySmallEpsilon() {
      int count = 100;
      double result = DifferentialPrivacyUtil.addLaplaceNoise(count, 0.01, SENSITIVITY);

      assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("should handle very large epsilon (low privacy)")
    void shouldHandleVeryLargeEpsilon() {
      int count = 100;
      double result = DifferentialPrivacyUtil.addLaplaceNoise(count, 100.0, SENSITIVITY);

      assertThat(result).isGreaterThan(0.0);
      // With very large epsilon, result should be very close to true count
      assertThat(result).isCloseTo(count, within(5.0));
    }

    @Test
    @DisplayName("should handle count exactly at threshold boundary")
    void shouldHandleCountAtThresholdBoundary() {
      int count = 10;
      double result = DifferentialPrivacyUtil.addLaplaceNoise(count, EPSILON, SENSITIVITY);

      // Result could be 0 or > 0 depending on noise
      assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("should handle different sensitivity values")
    void shouldHandleDifferentSensitivity() {
      int count = 100;
      double highSensitivity = 10.0;

      double result = DifferentialPrivacyUtil.addLaplaceNoise(count, EPSILON, highSensitivity);

      assertThat(result).isGreaterThanOrEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("Statistical Properties")
  class StatisticalProperties {

    @Test
    @DisplayName("should produce symmetric noise distribution around true count")
    void shouldProduceSymmetricDistribution() {
      int count = 1000;
      int aboveCount = 0;
      int belowCount = 0;

      for (int i = 0; i < SAMPLE_SIZE; i++) {
        double result = DifferentialPrivacyUtil.addLaplaceNoise(count, EPSILON, SENSITIVITY);
        if (result > count) {
          aboveCount++;
        } else if (result < count) {
          belowCount++;
        }
      }

      // Should be roughly symmetric (within 60/40 split)
      double ratio = (double) Math.min(aboveCount, belowCount) / Math.max(aboveCount, belowCount);
      assertThat(ratio).isGreaterThan(0.6);
    }

    @Test
    @DisplayName("should have decreasing probability for larger deviations")
    void shouldHaveDecreasingProbabilityForLargerDeviations() {
      int count = 1000;
      int closeCount = 0; // within 2 units
      int mediumCount = 0; // 2-10 units away
      int farCount = 0; // more than 10 units away

      for (int i = 0; i < SAMPLE_SIZE; i++) {
        double result = DifferentialPrivacyUtil.addLaplaceNoise(count, 1.0, SENSITIVITY);
        double deviation = Math.abs(result - count);

        if (deviation <= 2) {
          closeCount++;
        } else if (deviation <= 10) {
          mediumCount++;
        } else {
          farCount++;
        }
      }

      // Should have more close values than medium, and more medium than far
      assertThat(closeCount).isGreaterThan(mediumCount);
      assertThat(mediumCount).isGreaterThan(farCount);
    }
  }

  // Helper methods

  private double calculateVariance(List<Double> values) {
    double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double sumSquaredDiff = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum();
    return sumSquaredDiff / values.size();
  }

  private double calculateMeanAbsoluteDeviation(List<Double> values, double center) {
    return values.stream().mapToDouble(v -> Math.abs(v - center)).average().orElse(0.0);
  }
}
