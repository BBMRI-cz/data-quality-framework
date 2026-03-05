package eu.bbmri_eric.quality.agent.settings.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Enum representing the noise mechanism used for differential privacy. */
public enum NoiseMechanism {
  /**
   * Laplace mechanism - adds noise from a Laplace distribution. Best for unbounded queries or when
   * using (ε, 0)-differential privacy.
   */
  LAPLACE("LAPLACE"),

  /**
   * Gaussian mechanism - adds noise from a Gaussian (normal) distribution. Used for (ε,
   * δ)-differential privacy where δ > 0. Provides better utility for bounded queries with the same
   * privacy guarantee.
   */
  GAUSSIAN("GAUSSIAN");

  private final String value;

  NoiseMechanism(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static NoiseMechanism fromString(String value) {
    for (NoiseMechanism mechanism : NoiseMechanism.values()) {
      if (mechanism.value.equalsIgnoreCase(value)) {
        return mechanism;
      }
    }
    throw new IllegalArgumentException("Unknown noise mechanism: " + value);
  }
}
