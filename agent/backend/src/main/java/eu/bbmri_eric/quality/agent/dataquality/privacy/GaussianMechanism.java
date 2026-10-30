package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Gaussian mechanism for (ε, δ)-differential privacy with ε in (0, 1]. */
public final class GaussianMechanism implements Mechanism {

  private final RandomGenerator random;

  public GaussianMechanism(RandomGenerator random) {
    this.random = Objects.requireNonNull(random, "random");
  }

  @Override
  public double sample(int count, double epsilon, double delta, double sensitivity) {
    if (epsilon <= 0 || epsilon > 1.0) {
      throw new IllegalArgumentException("Epsilon must be in (0, 1], got: " + epsilon);
    }
    if (delta <= 0 || delta >= 1) {
      throw new IllegalArgumentException("Delta must be in (0, 1), got: " + delta);
    }
    if (sensitivity <= 0) {
      throw new IllegalArgumentException("Sensitivity must be positive, got: " + sensitivity);
    }
    return Math.max(
        0.0, count + random.nextGaussian() * standardDeviation(epsilon, delta, sensitivity));
  }

  /** σ = sensitivity · √(2·ln(1.25/δ)) / ε */
  static double standardDeviation(double epsilon, double delta, double sensitivity) {
    return sensitivity * Math.sqrt(2.0 * Math.log(1.25 / delta)) / epsilon;
  }
}
