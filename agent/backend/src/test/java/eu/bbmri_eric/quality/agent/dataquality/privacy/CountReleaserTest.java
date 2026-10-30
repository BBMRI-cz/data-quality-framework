package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class CountReleaserTest {

  private static final double MIN_THRESHOLD = 50;

  private CountReleaser releaserReturningNoNoise() {
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextDouble()).thenReturn(0.5); // u = 0.5 → Laplace noise 0
    return new CountReleaser(new LaplaceSampler(random), MIN_THRESHOLD);
  }

  @Test
  void release_withoutPrevious_drawsFreshNoise() {
    CountReleaser releaser = releaserReturningNoNoise();

    CountReleaser.Release release = releaser.release(100, null, 1.0, 1);

    assertThat(release.reused()).isFalse();
    assertThat(release.value()).isEqualTo(100.0);
    assertThat(release.epsilon()).isEqualTo(1.0);
  }

  @Test
  void release_withReusablePrevious_returnsOldValueAndEpsilonWithoutNoise() {
    CountReleaser releaser = releaserReturningNoNoise();
    PreviousRelease previous = new PreviousRelease(95.5, 100, 0.4);

    CountReleaser.Release release = releaser.release(104, previous, 1.0, 1);

    assertThat(release.reused()).isTrue();
    assertThat(release.value()).isEqualTo(95.5);
    assertThat(release.epsilon()).isEqualTo(0.4);
  }

  @Test
  void release_withDriftPastThreshold_drawsFreshNoise() {
    CountReleaser releaser = releaserReturningNoNoise();
    PreviousRelease previous = new PreviousRelease(95.5, 100, 0.4);

    CountReleaser.Release release = releaser.release(105, previous, 1.0, 1);

    assertThat(release.reused()).isFalse();
    assertThat(release.value()).isEqualTo(105.0);
  }

  @Test
  void release_withPreviousValueBelowSuppression_drawsFreshAndSuppresses() {
    CountReleaser releaser = releaserReturningNoNoise();
    PreviousRelease previous = new PreviousRelease(40.0, 40, 0.4);

    CountReleaser.Release release = releaser.release(40, previous, 1.0, 1);

    assertThat(release.reused()).isFalse();
    assertThat(release.value()).isEqualTo(0.0);
  }

  @Test
  void release_withZeroDrawnIsSuppressibleButReusableNextTime() {
    CountReleaser releaser = releaserReturningNoNoise();

    CountReleaser.Release release = releaser.release(3, null, 1.0, 1);

    assertThat(release.reused()).isFalse();
    assertThat(release.value()).isEqualTo(0.0);
  }
}
