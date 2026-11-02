package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class NoiseSamplerTest {

  @Test
  void sample_laplaceWithNonPositiveEpsilon_throws() {
    LaplaceSampler sampler = new LaplaceSampler(new SplittableRandom(1));
    assertThatThrownBy(() -> sampler.sample(100, 0.0, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Epsilon must be positive");
    assertThatThrownBy(() -> sampler.sample(100, Double.NaN, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Epsilon must be positive");
  }

  @Test
  void sample_laplaceWithNonPositiveSensitivity_throws() {
    LaplaceSampler sampler = new LaplaceSampler(new SplittableRandom(1));
    assertThatThrownBy(() -> sampler.sample(100, 1.0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Sensitivity must be positive");
  }

  @Test
  void sample_laplaceNoiseScaleMatchesEpsilonOverSensitivity() {
    // Seeded deterministic estimate of Laplace MAE: E|X| = b = sensitivity / epsilon.
    LaplaceSampler sampler = new LaplaceSampler(new SplittableRandom(20261030L));
    double sensitivity = 20;
    double epsilon = 0.1;
    double sumAbsError = 0;
    int samples = 20_000;
    for (int i = 0; i < samples; i++) {
      sumAbsError += Math.abs(sampler.sample(10_000, epsilon, sensitivity) - 10_000);
    }
    assertThat(sumAbsError / samples).isBetween(190.0, 210.0);
  }

  @Test
  void sample_gaussianRejectsEpsilonAboveOne() {
    GaussianSampler sampler = new GaussianSampler(new SplittableRandom(1), 1e-8);
    assertThatThrownBy(() -> sampler.sample(100, 1.1, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Epsilon must be in (0, 1]");
  }

  @Test
  void constructor_gaussianRejectsInvalidDelta() {
    assertThatThrownBy(() -> new GaussianSampler(new SplittableRandom(1), 0.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Delta must be in (0, 1)");
    assertThatThrownBy(() -> new GaussianSampler(new SplittableRandom(1), Double.NaN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Delta must be in (0, 1)");
  }

  @Test
  void sample_gaussianVarianceMatchesSigmaSquared() {
    // Correct expectation: variance of N(0, sigma) is sigma^2, not 2*sigma^2. At 50k samples the
    // estimate is far tighter than the ±20% tolerance this used to hide the wrong expectation
    // under.
    GaussianSampler sampler = new GaussianSampler(new SplittableRandom(7L), 1e-8);
    double sigma = GaussianSampler.standardDeviation(1.0, 1e-8, 1.0);
    double sumSqError = 0;
    int samples = 50_000;
    for (int i = 0; i < samples; i++) {
      double error = sampler.sample(1_000, 1.0, 1) - 1_000;
      sumSqError += error * error;
    }
    assertThat(sumSqError / samples).isBetween(0.97 * sigma * sigma, 1.03 * sigma * sigma);
  }

  @Test
  void of_selectsTheConfiguredSamplerExhaustively() {
    assertThat(NoiseSampler.of(NoiseMechanism.LAPLACE, new SplittableRandom(1), 1e-8))
        .isInstanceOf(LaplaceSampler.class);
    assertThat(NoiseSampler.of(NoiseMechanism.GAUSSIAN, new SplittableRandom(1), 1e-8))
        .isInstanceOf(GaussianSampler.class);
  }
}
