package eu.bbmri_eric.quality.agent.dataquality;

import java.security.SecureRandom;

/**
 * Applies differential privacy using Laplace or Gaussian mechanisms with low count suppression.
 *
 * <p>Noise is added to counts, then noisy counts below the threshold are suppressed. This maintains
 * differential privacy while protecting low counts.
 */
public class DifferentialPrivacyUtil {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static double LOW_COUNT_THRESHOLD = 10.0;

  /**
   * Sets the low-count suppression threshold.
   *
   * @param threshold the new threshold (must be non-negative)
   */
  public static synchronized void setLowCountThreshold(double threshold) {
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold must be non-negative, got: " + threshold);
    }
    LOW_COUNT_THRESHOLD = threshold;
  }

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
   * Adds Gaussian noise to a count for (ε, δ)-differential privacy, then suppresses if below
   * threshold.
   *
   * @param count the original count
   * @param epsilon the privacy budget (must be positive)
   * @param delta the probability of privacy failure (must be positive and less than 1)
   * @param sensitivity the query sensitivity (must be positive)
   * @return noisy count clamped at 0, or 0 if below threshold
   */
  public static double addGaussianNoise(
      int count, double epsilon, double delta, double sensitivity) {
    if (epsilon <= 0) {
      throw new IllegalArgumentException("Epsilon must be positive, got: " + epsilon);
    }
    if (delta <= 0 || delta >= 1) {
      throw new IllegalArgumentException("Delta must be in (0, 1), got: " + delta);
    }
    if (sensitivity <= 0) {
      throw new IllegalArgumentException("Sensitivity must be positive, got: " + sensitivity);
    }

    double standardDeviation = sensitivity * Math.sqrt(2.0 * Math.log(1.25 / delta)) / epsilon;
    double noise = SECURE_RANDOM.nextGaussian() * standardDeviation;
    double noisyCount = Math.max(0.0, count + noise);

    if (noisyCount < LOW_COUNT_THRESHOLD) {
      return 0.0;
    }

    return noisyCount;
  }

  /**
   * Returns the standard deviation used by the Gaussian mechanism for the given parameters.
   *
   * @param epsilon the privacy budget
   * @param delta the probability of privacy failure
   * @param sensitivity the query sensitivity
   * @return σ = sensitivity * sqrt(2 * ln(1.25/δ)) / ε
   */
  public static double calculateGaussianStandardDeviation(
      double epsilon, double delta, double sensitivity) {
    return sensitivity * Math.sqrt(2.0 * Math.log(1.25 / delta)) / epsilon;
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
