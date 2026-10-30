package eu.bbmri_eric.quality.agent.dataquality.privacy;

import eu.bbmri_eric.quality.agent.dataquality.privacy.NoiseHistory.PreviousRelease;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * The single place that decides between reusing the previously published noisy value and drawing
 * fresh noise for a count. Reuse is allowed when {@link PreviousRelease#reusableFor} accepts the
 * new raw count; otherwise fresh noise is drawn through the configured mechanism and suppressed
 * below the minimum threshold.
 */
public final class CountReleaser {

  private final PrivacyParameters parameters;
  private final Mechanism mechanism;

  /**
   * Creates a releaser with an explicitly chosen mechanism. Callers pass the mechanism built from
   * {@link Mechanism#of} — the entity totals always use Laplace, check results use the configured
   * mechanism — so the choice is visible at the call site instead of being decided implicitly.
   */
  public CountReleaser(PrivacyParameters parameters, Mechanism mechanism) {
    this.parameters = Objects.requireNonNull(parameters, "parameters");
    this.mechanism = Objects.requireNonNull(mechanism, "mechanism");
  }

  /** Creates a releaser using the mechanism configured in the privacy parameters. */
  public static CountReleaser forParameters(PrivacyParameters parameters, RandomGenerator random) {
    return new CountReleaser(
        parameters, Mechanism.of(parameters.mechanism(), Objects.requireNonNull(random, "random")));
  }

  /** The outcome of one release: the published value and whether it was reused. */
  public record Release(double value, boolean reused) {}

  /**
   * Releases a count: reuses the previous value when allowed by {@code
   * refreshThreshold(sensitivity)} and suppression rules, otherwise draws fresh noise with the
   * given per-release epsilon.
   */
  public Release release(int count, PreviousRelease previous, double epsilon, int sensitivity) {
    if (previous != null
        && previous.reusableFor(
            count, PrivacyBudget.refreshThreshold(sensitivity), parameters.minThreshold())) {
      return new Release(previous.published(), true);
    }
    return new Release(freshValue(count, epsilon, sensitivity), false);
  }

  /** Draws fresh noise for a count and applies low-count suppression. */
  public double freshValue(int count, double epsilon, int sensitivity) {
    return LowCountSuppression.apply(
        mechanism.sample(count, epsilon, parameters.delta(), sensitivity),
        parameters.minThreshold());
  }
}
