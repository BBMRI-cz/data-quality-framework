package eu.bbmri_eric.quality.agent.dataquality.privacy;

/**
 * The reconstructed noise state of a count: the value published most recently, the anchor that
 * value was drawn for, and the epsilon used for it. Reconstructed by {@link
 * NoiseHistory#replay(java.util.List, int)} from the publication history — nothing is stored
 * separately.
 */
public record PreviousRelease(double published, int anchor, Double epsilon) {

  /**
   * The single reuse rule, shared by check results and entity totals: the new raw count stayed
   * within {@code refreshThreshold} of the anchor, and the reused value does not republish a
   * non-zero count below the current minimum suppression threshold (a suppressed zero may still be
   * reused).
   */
  public boolean reusableFor(int raw, int refreshThreshold, double minThreshold) {
    return Math.abs(raw - anchor) < refreshThreshold
        && (published == 0.0 || published >= minThreshold);
  }
}
