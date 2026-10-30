package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Laplace mechanism for (ε, 0)-differential privacy; the δ argument is unused. */
public final class LaplaceMechanism implements Mechanism {

  private final RandomGenerator random;

  public LaplaceMechanism(RandomGenerator random) {
    this.random = Objects.requireNonNull(random, "random");
  }

  @Override
  public double sample(int count, double epsilon, double delta, double sensitivity) {
    if (epsilon <= 0) {
      throw new IllegalArgumentException("Epsilon must be positive, got: " + epsilon);
    }
    if (sensitivity <= 0) {
      throw new IllegalArgumentException("Sensitivity must be positive, got: " + sensitivity);
    }
    double scale = sensitivity / epsilon;
    double u = random.nextDouble() - 0.5;
    double noise = -scale * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
    return Math.max(0.0, count + noise);
  }
}
