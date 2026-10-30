package eu.bbmri_eric.quality.agent.dataquality.privacy;

/**
 * Privacy-budget policy for a single report: how the report's epsilon budget is split between check
 * results and entity totals, and how far a count must drift from its anchor before fresh noise is
 * drawn.
 */
public final class PrivacyBudget {

  /** Fraction reserved for the entity totals (each total consumes half of this share). */
  private static final double ENTITY_TOTALS_EPSILON_FRACTION = 0.2;

  /** Base drift (in units of sensitivity 1) that triggers fresh noise. */
  private static final int NOISE_REFRESH_THRESHOLD = 5;

  private PrivacyBudget() {}

  /** Epsilon share of a report's budget that is split among the quality check results (80%). */
  public static double epsilonForCheckResults(double totalEpsilon) {
    return totalEpsilon * (1.0 - ENTITY_TOTALS_EPSILON_FRACTION);
  }

  /** Epsilon share per entity total (half of the totals' 20% share). */
  public static double epsilonPerEntityTotal(double totalEpsilon) {
    return totalEpsilon * ENTITY_TOTALS_EPSILON_FRACTION / 2;
  }

  /**
   * Minimum drift from the anchor that triggers a fresh draw for a count with the given
   * sensitivity. Scaling keeps one individual's contribution below the threshold (e.g. for the
   * sample total, one donation of up to 20 samples never becomes visible on its own). The same
   * threshold drives both the history replay and the reuse decision via {@link
   * PreviousRelease#reusableFor}.
   */
  public static int refreshThreshold(int sensitivity) {
    return NOISE_REFRESH_THRESHOLD * sensitivity;
  }
}
