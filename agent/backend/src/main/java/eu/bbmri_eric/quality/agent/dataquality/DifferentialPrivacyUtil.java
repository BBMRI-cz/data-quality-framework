package eu.bbmri_eric.quality.agent.dataquality;

import java.security.SecureRandom;

/**
 * Applies differential privacy using the Laplace mechanism with low count suppression.
 *
 * <p>Noise is always added to counts (scale λ = sensitivity/ε), then noisy counts below the
 * threshold are suppressed. This maintains ε-differential privacy while protecting low counts.
 */
public class DifferentialPrivacyUtil {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final double LOW_COUNT_THRESHOLD = 10.0;

  /**
   * Adds Laplace noise to a count, then suppresses if below threshold.
   *
   * @param count the original count
   * @param epsilon the privacy budget (smaller means more noise, must be positive)
   * @param sensitivity the query sensitivity (must be positive)
   * @return noisy count clamped at 0, or 0 if below threshold
   */
  public static double addLaplaceNoise(int count, double epsilon, double sensitivity) {
    if (epsilon <= 0) {
      throw new IllegalArgumentException("Epsilon must be positive, got: " + epsilon);
    }
    if (sensitivity <= 0) {
      throw new IllegalArgumentException("Sensitivity must be positive, got: " + sensitivity);
    }

    double scale = sensitivity / epsilon;
    double noise = generateLaplaceNoise(scale);
    double noisyCount = Math.max(0.0, count + noise);

    if (noisyCount < LOW_COUNT_THRESHOLD) {
      return 0.0;
    }

    return noisyCount;
  }

  /**
   * Generates Laplace-distributed noise using inverse transform sampling.
   *
   * @param scale the scale parameter
   * @return sample from Laplace(0, scale)
   */
  private static double generateLaplaceNoise(double scale) {
    double u = SECURE_RANDOM.nextDouble() - 0.5;
    return -scale * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
  }
}
