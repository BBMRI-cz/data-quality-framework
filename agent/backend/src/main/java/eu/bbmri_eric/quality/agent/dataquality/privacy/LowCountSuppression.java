package eu.bbmri_eric.quality.agent.dataquality.privacy;

/** Low-count suppression: noisy counts below the configured threshold are published as zero. */
public final class LowCountSuppression {

  private LowCountSuppression() {}

  /**
   * Suppresses a noisy count below the threshold.
   *
   * @param noisyCount the noisy count (already clamped at zero by the mechanism)
   * @param threshold the minimum publishable count (non-negative)
   * @return the noisy count, or {@code 0.0} when below the threshold
   */
  public static double apply(double noisyCount, double threshold) {
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold must be non-negative, got: " + threshold);
    }
    return noisyCount < threshold ? 0.0 : noisyCount;
  }
}
