package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import org.junit.jupiter.api.Test;

class PrivacyParametersTest {

  @Test
  void from_mapsSettingsAndDefaultsMechanismToLaplace() {
    SettingsDTO settings = SettingsDTO.builder().epsilon(3.0).delta(1e-8).minThreshold(50).build();
    PrivacyParameters parameters = PrivacyParameters.from(settings);
    assertThat(parameters.epsilon()).isEqualTo(3.0);
    assertThat(parameters.delta()).isEqualTo(1e-8);
    assertThat(parameters.minThreshold()).isEqualTo(50.0);
    assertThat(parameters.mechanism()).isEqualTo(NoiseMechanism.LAPLACE);
  }

  @Test
  void constructor_withEpsilonBelowMinimum_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(0.05, 1e-8, 50, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Epsilon must be in [0.1, 10.0]");
  }

  @Test
  void constructor_withEpsilonAboveMaximum_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(10.5, 1e-8, 50, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Epsilon must be in [0.1, 10.0]");
  }

  @Test
  void constructor_withNaNInputs_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(Double.NaN, 1e-8, 50, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PrivacyParameters(1.0, Double.NaN, 50, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void constructor_withDeltaAboveMaximum_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(1.0, 1e-4, 50, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Delta must be in (0, 1.0E-5]");
  }

  @Test
  void constructor_withThresholdBelowMinimum_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(1.0, 1e-8, 9, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Threshold must be at least 10.0");
  }

  @Test
  void constructor_withNullMechanism_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(1.0, 1e-8, 50, null))
        .isInstanceOf(NullPointerException.class);
  }
}
