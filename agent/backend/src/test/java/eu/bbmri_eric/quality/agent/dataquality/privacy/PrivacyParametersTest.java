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
    assertThat(parameters.minThreshold()).isEqualTo(50);
    assertThat(parameters.mechanism()).isEqualTo(NoiseMechanism.LAPLACE);
  }

  @Test
  void constructor_withNonPositiveEpsilon_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(0.0, 1e-8, 50, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Epsilon must be positive");
  }

  @Test
  void constructor_withDeltaOutOfRange_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(1.0, 1.0, 50, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Delta must be in (0, 1)");
  }

  @Test
  void constructor_withNegativeThreshold_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(1.0, 1e-8, -1, NoiseMechanism.LAPLACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Threshold must be non-negative");
  }

  @Test
  void constructor_withNullMechanism_throws() {
    assertThatThrownBy(() -> new PrivacyParameters(1.0, 1e-8, 50, null))
        .isInstanceOf(NullPointerException.class);
  }
}
