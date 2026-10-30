package eu.bbmri_eric.quality.agent.dataquality.privacy;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import java.util.random.RandomGenerator;

/**
 * A differential-privacy noise mechanism. Implementations take an injected {@link RandomGenerator}
 * so tests can be deterministic.
 */
public sealed interface Mechanism permits LaplaceMechanism, GaussianMechanism {

  /**
   * Draws one noisy count (clamped at zero) for the given raw value and sensitivity.
   *
   * @param count the raw count (non-negative)
   * @param epsilon the privacy budget allocated to this one release (positive)
   * @param delta the δ for (ε, δ)-privacy; ignored by pure-ε mechanisms
   * @param sensitivity how much one individual can change the count (positive)
   * @return the noisy count, clamped at 0
   */
  double sample(int count, double epsilon, double delta, double sensitivity);

  /**
   * Builds the mechanism once for the configured type. The switch is exhaustive: adding a new
   * {@link NoiseMechanism} without handling it here fails compilation instead of silently falling
   * back to a default.
   */
  static Mechanism of(NoiseMechanism type, RandomGenerator random) {
    return switch (type) {
      case LAPLACE -> new LaplaceMechanism(random);
      case GAUSSIAN -> new GaussianMechanism(random);
    };
  }
}
