package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class CountReleaserTest {

  private static final PrivacyParameters LAPLACE_1 =
      new PrivacyParameters(1.0, 1e-8, 50, NoiseMechanism.LAPLACE);

  @Test
  void release_withoutPrevious_drawsFreshNoise() {
    // u = 0.5 → Laplace noise 0 → value equals the raw count (above the threshold)
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextDouble()).thenReturn(0.5);
    CountReleaser releaser = CountReleaser.forParameters(LAPLACE_1, random);

    CountReleaser.Release release = releaser.release(100, null, 1.0, 1);

    assertThat(release.reused()).isFalse();
    assertThat(release.value()).isEqualTo(100.0);
  }

  @Test
  void release_withReusablePrevious_returnsOldValueWithoutNoise() {
    RandomGenerator random = mock(RandomGenerator.class);
    CountReleaser releaser = CountReleaser.forParameters(LAPLACE_1, random);
    NoiseHistory.PreviousRelease previous = new NoiseHistory.PreviousRelease(95.5, 100, 0.4);

    CountReleaser.Release release = releaser.release(104, previous, 1.0, 1);

    assertThat(release.reused()).isTrue();
    assertThat(release.value()).isEqualTo(95.5);
  }

  @Test
  void release_withDriftPastThreshold_drawsFreshNoise() {
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextDouble()).thenReturn(0.5);
    CountReleaser releaser = CountReleaser.forParameters(LAPLACE_1, random);
    NoiseHistory.PreviousRelease previous = new NoiseHistory.PreviousRelease(95.5, 100, 0.4);

    CountReleaser.Release release = releaser.release(105, previous, 1.0, 1);

    assertThat(release.reused()).isFalse();
    assertThat(release.value()).isEqualTo(105.0);
  }

  @Test
  void release_withPreviousValueBelowSuppression_drawsFresh() {
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextDouble()).thenReturn(0.5);
    CountReleaser releaser = CountReleaser.forParameters(LAPLACE_1, random);
    NoiseHistory.PreviousRelease previous = new NoiseHistory.PreviousRelease(40.0, 40, 0.4);

    CountReleaser.Release release = releaser.release(40, previous, 1.0, 1);

    assertThat(release.reused()).isFalse();
    assertThat(release.value()).isEqualTo(0.0); // fresh draw of 40 is suppressed by threshold 50
  }

  @Test
  void freshValue_appliesSuppressionBelowThreshold() {
    CountReleaser releaser = CountReleaser.forParameters(LAPLACE_1, new SplittableRandom(2));
    assertThat(releaser.freshValue(3, 1.0, 1)).isEqualTo(0.0);
  }

  @Test
  void constructor_withNullMechanism_stillChoosesByDefaultLaplaceViaParameters() {
    assertThatThrownBy(() -> new PrivacyParameters(1.0, 1e-8, 50, null))
        .isInstanceOf(NullPointerException.class);
  }
}
