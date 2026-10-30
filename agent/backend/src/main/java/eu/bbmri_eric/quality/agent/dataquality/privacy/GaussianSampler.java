package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Gaussian sampler for (ε, δ)-differential privacy with ε in (0, 1]. Returns unclamped draws. */
public final class GaussianSampler implements NoiseSampler {

  private final RandomGenerator random;
  private final double delta;

  /**
   * @param random the randomness source
   * @param delta the δ for (ε, δ)-privacy, validated once here instead of on every draw
   */
  public GaussianSampler(RandomGenerator random, double delta) {
    this.random = Objects.requireNonNull(random, "random");
    if (!(delta > 0 && delta < 1)) {
      throw new IllegalArgumentException("Delta must be in (0, 1), got: " + delta);
    }
    this.delta = delta;
  }

  @Override
  public double sample(int count, double epsilon, double sensitivity) {
    if (!(epsilon > 0 && epsilon <= 1.0)) {
      throw new IllegalArgumentException("Epsilon must be in (0, 1], got: " + epsilon);
    }
    if (!(sensitivity > 0)) {
      throw new IllegalArgumentException("Sensitivity must be positive, got: " + sensitivity);
    }
    return count + random.nextGaussian() * standardDeviation(epsilon, delta, sensitivity);
  }

  /** σ = sensitivity · √(2·ln(1.25/δ)) / ε */
  static double standardDeviation(double epsilon, double delta, double sensitivity) {
    return sensitivity * Math.sqrt(2.0 * Math.log(1.25 / delta)) / epsilon;
  }
}
