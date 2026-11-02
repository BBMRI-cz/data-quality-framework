package eu.bbmri_eric.quality.agent.dataquality.privacy;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.Objects;

/**
 * Validated privacy parameters for one release round, derived once from the settings record.
 * Construction fails fast on invalid input — including values stored before these limits were
 * introduced — so downstream code can rely on them. Numeric guards intentionally use negated
 * comparisons so {@code NaN} can never slip through (every comparison with {@code NaN} is false).
 */
public record PrivacyParameters(
    double epsilon, double delta, double minThreshold, NoiseMechanism mechanism) {

  private static final double MIN_EPSILON = 0.1;
  private static final double MAX_EPSILON = 10.0;
  private static final double MAX_DELTA = 1e-5;
  private static final double MIN_THRESHOLD = 10.0;

  public PrivacyParameters {
    if (!(epsilon >= MIN_EPSILON && epsilon <= MAX_EPSILON)) {
      throw new IllegalArgumentException(
          "Epsilon must be in [%s, %s], got: %s".formatted(MIN_EPSILON, MAX_EPSILON, epsilon));
    }
    if (!(delta > 0 && delta <= MAX_DELTA)) {
      throw new IllegalArgumentException(
          "Delta must be in (0, %s], got: %s".formatted(MAX_DELTA, delta));
    }
    if (!(minThreshold >= MIN_THRESHOLD)) {
      throw new IllegalArgumentException(
          "Threshold must be at least %s, got: %s".formatted(MIN_THRESHOLD, minThreshold));
    }
    Objects.requireNonNull(mechanism, "Noise mechanism must not be null");
  }

  public static PrivacyParameters from(SettingsDTO settings) {
    Integer minThreshold = settings.getMinThreshold();
    return new PrivacyParameters(
        settings.getEpsilon(),
        settings.getDelta(),
        minThreshold != null ? minThreshold.doubleValue() : Double.NaN,
        settings.getNoiseMechanism() != null
            ? settings.getNoiseMechanism()
            : NoiseMechanism.LAPLACE);
  }
}
