package eu.bbmri_eric.quality.agent.dataquality.privacy;

/**
 * Post-processing for noise draws: clamps at zero and suppresses below the minimum count. One place
 * for both steps so samplers never half-do it themselves.
 */
public final class LowCountSuppression {

  private LowCountSuppression() {}

  /**
   * Publishes a draw after clamping and suppression: values are first clamped at zero (samplers may
   * draw below it), then suppresses anything below the threshold to {@code 0.0}.
   *
   * @param noisyDraw the draw as produced by a {@link NoiseSampler}
   * @param threshold the minimum publishable count (non-negative)
   * @return the clamped draw, or {@code 0.0} when below the threshold
   */
  public static double apply(double noisyDraw, double threshold) {
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold must be non-negative, got: " + threshold);
    }
    double clamped = Math.max(0.0, noisyDraw);
    return clamped < threshold ? 0.0 : clamped;
  }
}
