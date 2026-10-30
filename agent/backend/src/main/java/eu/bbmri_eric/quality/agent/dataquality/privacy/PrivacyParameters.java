package eu.bbmri_eric.quality.agent.dataquality.privacy;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.Objects;

/**
 * Validated privacy parameters for one release round, derived once from the settings record.
 * Construction fails fast on invalid input, so downstream mechanisms never re-check these
 * invariants.
 */
public record PrivacyParameters(
    double epsilon, double delta, double minThreshold, NoiseMechanism mechanism) {

  public PrivacyParameters {
    if (epsilon <= 0) {
      throw new IllegalArgumentException("Epsilon must be positive, got: " + epsilon);
    }
    if (delta <= 0 || delta >= 1) {
      throw new IllegalArgumentException("Delta must be in (0, 1), got: " + delta);
    }
    if (minThreshold < 0) {
      throw new IllegalArgumentException("Threshold must be non-negative, got: " + minThreshold);
    }
    Objects.requireNonNull(mechanism, "Noise mechanism must not be null");
  }

  public static PrivacyParameters from(SettingsDTO settings) {
    return new PrivacyParameters(
        settings.getEpsilon(),
        settings.getDelta(),
        settings.getMinThreshold(),
        settings.getNoiseMechanism() != null
            ? settings.getNoiseMechanism()
            : NoiseMechanism.LAPLACE);
  }
}
