package eu.bbmri_eric.quality.agent.report;

import java.util.Random;

/**
 * Utility class for applying differential privacy techniques to count data.
 *
 * <p>This class provides a method to add Laplace noise to a given count in accordance with
 * differential privacy principles. The amount of noise is determined by the specified epsilon and
 * sensitivity parameters.
 */
class DifferentialPrivacyUtil {

  /** Random number generator used for noise generation. */
  private static final Random random = new Random();

  /**
   * Adds Laplace noise to the given count using the specified privacy parameters.
   *
   * @param count the original count to which noise should be added
   * @param epsilon the privacy budget (smaller epsilon means more noise)
   * @param sensitivity the sensitivity of the query (maximum change in output for one record)
   * @return a noisy version of the count, rounded to the nearest integer and clamped at 0
   */
  static int addLaplaceNoise(int count, double epsilon, double sensitivity) {
    if (count != 0) {
      double scale = sensitivity / epsilon;
      double noise = generateLaplaceNoise(scale);
      double noisyCount = count + noise;
      return Math.max(0, (int) Math.round(noisyCount));
    } else {
      return 0;
    }
  }

  /**
   * Generates Laplace-distributed noise with the specified scale parameter.
   *
   * <p>The Laplace distribution is centered at 0 and has variance 2 * scale².
   *
   * @param scale the scale (b) of the Laplace distribution
   * @return a sample from the Laplace distribution with mean 0 and scale {@code scale}
   */
  private static double generateLaplaceNoise(double scale) {
    double u = random.nextDouble() - 0.5; // Uniform random variable in [-0.5, 0.5]
    return -scale * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
  }
}
