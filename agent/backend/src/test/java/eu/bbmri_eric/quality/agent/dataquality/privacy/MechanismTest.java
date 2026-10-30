package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class MechanismTest {

  @Test
  void sample_laplaceWithZeroEpsilon_throws() {
    LaplaceMechanism mechanism = new LaplaceMechanism(new SplittableRandom(1));
    assertThatThrownBy(() -> mechanism.sample(100, 0.0, 1e-8, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Epsilon must be positive");
  }

  @Test
  void sample_laplaceWithNonPositiveSensitivity_throws() {
    LaplaceMechanism mechanism = new LaplaceMechanism(new SplittableRandom(1));
    assertThatThrownBy(() -> mechanism.sample(100, 1.0, 1e-8, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Sensitivity must be positive");
  }

  @Test
  void sample_laplaceNeverReturnsNegative() {
    LaplaceMechanism mechanism = new LaplaceMechanism(new SplittableRandom(42));
    for (int i = 0; i < 1000; i++) {
      assertThat(mechanism.sample(5, 0.1, 1e-8, 1)).isGreaterThanOrEqualTo(0.0);
    }
  }

  @Test
  void sample_laplaceNoiseScaleMatchesEpsilonAndSensitivity() {
    // Seeded run is deterministic: MAE of Laplace(0, sensitivity / epsilon) converges to that
    // scale.
    LaplaceMechanism mechanism = new LaplaceMechanism(new SplittableRandom(20261030L));
    double sensitivity = 20;
    double epsilon = 0.1;
    double sumAbsError = 0;
    int samples = 20_000;
    for (int i = 0; i < samples; i++) {
      sumAbsError += Math.abs(mechanism.sample(10_000, epsilon, 1e-8, sensitivity) - 10_000);
    }
    double meanAbsoluteError = sumAbsError / samples;
    assertThat(meanAbsoluteError)
        .as("Laplace MAE should equal b = sensitivity/epsilon = %s", sensitivity / epsilon)
        .isBetween(190.0, 210.0);
  }

  @Test
  void sample_gaussianRejectsEpsilonAboveOne() {
    GaussianMechanism mechanism = new GaussianMechanism(new SplittableRandom(1));
    assertThatThrownBy(() -> mechanism.sample(100, 1.1, 1e-8, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Epsilon must be in (0, 1]");
  }

  @Test
  void sample_gaussianVarianceMatchesSigmaSquared() {
    // Correct expectation: variance of N(0, sigma) is sigma^2, NOT 2*sigma^2.
    GaussianMechanism mechanism = new GaussianMechanism(new SplittableRandom(7L));
    double epsilon = 1.0;
    double delta = 1e-8;
    double sensitivity = 1.0;
    double sigma = GaussianMechanism.standardDeviation(epsilon, delta, sensitivity);
    double sumSqError = 0;
    int samples = 50_000;
    for (int i = 0; i < samples; i++) {
      double error = mechanism.sample(1_000, epsilon, delta, sensitivity) - 1_000;
      sumSqError += error * error;
    }
    double variance = sumSqError / samples;
    assertThat(variance).isBetween(0.8 * sigma * sigma, 1.2 * sigma * sigma);
  }

  @Test
  void of_selectsTheConfiguredMechanismExhaustively() {
    PrivacyParameters laplace = new PrivacyParameters(1.0, 1e-8, 50, NoiseMechanism.LAPLACE);
    PrivacyParameters gaussian = new PrivacyParameters(1.0, 1e-8, 50, NoiseMechanism.GAUSSIAN);
    assertThat(Mechanism.of(laplace.mechanism(), new SplittableRandom(1)))
        .isInstanceOf(LaplaceMechanism.class);
    assertThat(Mechanism.of(gaussian.mechanism(), new SplittableRandom(1)))
        .isInstanceOf(GaussianMechanism.class);
  }
}
