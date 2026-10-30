package eu.bbmri_eric.quality.agent.dataquality.privacy;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import java.util.random.RandomGenerator;

/**
 * A differential-privacy noise sampler. Implementations take an injected {@link RandomGenerator}
 * (and δ directly for Gaussian) so tests can be deterministic. Samplers return raw draws; clamping
 * and suppression is centralized in {@link LowCountSuppression}.
 */
public sealed interface NoiseSampler permits LaplaceSampler, GaussianSampler {

  /**
   * Draws one noisy count for the given raw value and sensitivity. The result is not clamped; pass
   * it through {@link LowCountSuppression} before publishing.
   *
   * @param count the raw count (non-negative)
   * @param epsilon the privacy budget allocated to this one release (positive)
   * @param sensitivity how much one individual can change the count (positive)
   * @return the noisy count as drawn (may be below zero)
   */
  double sample(int count, double epsilon, double sensitivity);

  /**
   * Builds the sampler once for the configured type. The switch is exhaustive: adding a new {@link
   * NoiseMechanism} without handling it here fails compilation instead of silently falling back to
   * a default. δ is consumed only by the Gaussian sampler.
   */
  static NoiseSampler of(NoiseMechanism type, RandomGenerator random, double delta) {
    return switch (type) {
      case LAPLACE -> new LaplaceSampler(random);
      case GAUSSIAN -> new GaussianSampler(random, delta);
    };
  }
}
