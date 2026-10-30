package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.util.Objects;

/**
 * The single place that decides between reusing the previously published noisy value and drawing
 * fresh noise for a count. It holds only what each release needs — one explicit sampler and the
 * suppression threshold — so it can never carry a contradictory (e.g. Gaussian-configured but
 * Laplace-sampling) state.
 */
public final class CountReleaser {

  /** The outcome of one release: the published value, the epsilon spent, and reuse flag. */
  public record Release(double value, double epsilon, boolean reused) {}

  private final NoiseSampler sampler;
  private final double minThreshold;

  public CountReleaser(NoiseSampler sampler, double minThreshold) {
    this.sampler = Objects.requireNonNull(sampler, "sampler");
    this.minThreshold = minThreshold;
  }

  /**
   * Releases a count: reuses the previous value when {@link PreviousRelease#reusableFor} accepts
   * it, otherwise draws fresh noise with the given per-release epsilon and the sampler selected at
   * construction. Results capable of reuse carry the epsilon of the previous release; fresh draws
   * carry the epsilon of this draw.
   */
  public Release release(int count, PreviousRelease previous, double epsilon, int sensitivity) {
    if (previous != null
        && previous.reusableFor(count, PrivacyBudget.refreshThreshold(sensitivity), minThreshold)) {
      // Check results track per-release epsilon and reuse carries it; totals carry none, so the
      // reuse still "spends" the budget reserved for this release.
      double spentEpsilon = previous.epsilon() != null ? previous.epsilon() : epsilon;
      return new Release(previous.published(), spentEpsilon, true);
    }
    double draw = sampler.sample(count, epsilon, sensitivity);
    return new Release(LowCountSuppression.apply(draw, minThreshold), epsilon, false);
  }
}
