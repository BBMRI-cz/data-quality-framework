package eu.bbmri_eric.quality.agent.report;

import java.util.Random;

public class LaplaceNoise {
  private static final Random random = new Random();

  public static int addLaplaceNoise(int count, double epsilon, double sensitivity) {
    double scale = sensitivity / epsilon;
    double noise = generateLaplaceNoise(scale);
    double noisyCount = count + noise;
    return Math.max(0, (int) Math.round(noisyCount));
  }

  private static double generateLaplaceNoise(double scale) {
    double u = random.nextDouble() - 0.5; // Uniform random variable in [-0.5, 0.5]
    return -scale * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
  }
}
