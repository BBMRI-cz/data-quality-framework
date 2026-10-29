package eu.bbmri_eric.quality.agent.dataquality;

import java.security.SecureRandom;

/**
 * Applies differential privacy using Laplace or Gaussian mechanisms with low count suppression.
 *
 * <p>Noise is added to counts, then noisy counts below the given threshold are suppressed. This
 * maintains differential privacy while protecting low counts. The suppression threshold is passed
 * explicitly on every call so that concurrent reports never share mutable state.
 */
public class DifferentialPrivacyUtil {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * Fraction of a report's epsilon budget reserved for noising the entity totals (patient count and
   * secondary entity count). Each total consumes half of this share. Reserving part of the budget
   * up front keeps the total privacy loss of a single report within its epsilon budget.
   */
  public static final double ENTITY_TOTALS_EPSILON_FRACTION = 0.2;

  /** Fraction of a report's epsilon budget available for noising quality check results. */
  public static final double CHECK_RESULTS_EPSILON_FRACTION = 1.0 - ENTITY_TOTALS_EPSILON_FRACTION;

  /**
   * Minimum absolute drift of a raw count away from the value its current noise was drawn for (its
   * anchor) before fresh noise is sampled. Smaller drifts reuse the previous noisy value, so
   * patient-level changes neither become observable as change events on the server nor can be
   * averaged across repeated reports.
   */
  public static final int NOISE_REFRESH_THRESHOLD = 5;

  /**
   * Adds Laplace noise to a count, then suppresses if below the given threshold.
   *
   * @param count the original count
   * @param epsilon the privacy budget (smaller means more noise, must be positive)
   * @param sensitivity the query sensitivity (must be positive)
   * @param threshold the low count suppression threshold (must be non-negative)
   * @return noisy count clamped at 0, or 0 if below threshold
   */
  public static double addLaplaceNoise(
      int count, double epsilon, double sensitivity, double threshold) {
    if (epsilon <= 0) {
      throw new IllegalArgumentException("Epsilon must be positive, got: " + epsilon);
    }
    if (sensitivity <= 0) {
      throw new IllegalArgumentException("Sensitivity must be positive, got: " + sensitivity);
    }
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold must be non-negative, got: " + threshold);
    }

    double scale = sensitivity / epsilon;
    double noise = generateLaplaceNoise(scale);
    double noisyCount = Math.max(0.0, count + noise);

    if (noisyCount < threshold) {
      return 0.0;
    }

    return noisyCount;
  }

  /**
   * Adds Gaussian noise to a count for (ε, δ)-differential privacy, then suppresses if below the
   * given threshold.
   *
   * @param count the original count
   * @param epsilon the privacy budget (must be positive)
   * @param delta the probability of privacy failure (must be positive and less than 1)
   * @param sensitivity the query sensitivity (must be positive)
   * @param threshold the low count suppression threshold (must be non-negative)
   * @return noisy count clamped at 0, or 0 if below threshold
   */
  public static double addGaussianNoise(
      int count, double epsilon, double delta, double sensitivity, double threshold) {
    if (epsilon <= 0 || epsilon > 1.0) {
      throw new IllegalArgumentException("Epsilon must be in (0, 1], got: " + epsilon);
    }
    if (delta <= 0 || delta >= 1) {
      throw new IllegalArgumentException("Delta must be in (0, 1), got: " + delta);
    }
    if (sensitivity <= 0) {
      throw new IllegalArgumentException("Sensitivity must be positive, got: " + sensitivity);
    }
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold must be non-negative, got: " + threshold);
    }

    double standardDeviation = sensitivity * Math.sqrt(2.0 * Math.log(1.25 / delta)) / epsilon;
    double noise = SECURE_RANDOM.nextGaussian() * standardDeviation;
    double noisyCount = Math.max(0.0, count + noise);

    if (noisyCount < threshold) {
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
