package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NoiseAnchorsTest {

  private static final int THRESHOLD = DifferentialPrivacyUtil.NOISE_REFRESH_THRESHOLD;

  private static NoiseAnchors.HistoryEntry<Double> entry(Integer raw, Double published) {
    return new NoiseAnchors.HistoryEntry<>(raw, published, 0.5);
  }

  @Test
  void reconstruct_withSuppressedRun_replaysAnchorToLastFreshDraw() {
    // All fresh draws below the suppression threshold published 0.0; the anchor must replay
    // forward through the equal-value run instead of returning the oldest raw value (20).
    List<NoiseAnchors.HistoryEntry<Double>> history =
        List.of(entry(27, 0.0), entry(25, 0.0), entry(22, 0.0), entry(20, 0.0));

    NoiseAnchors.PreviousNoise<Double> noise = NoiseAnchors.reconstruct(history, THRESHOLD);

    assertThat(noise.published()).isEqualTo(0.0);
    assertThat(noise.anchor()).isEqualTo(25);
  }

  @Test
  void reconstruct_withRoundingCollision_replaysAnchorToLastFreshDraw() {
    // Two consecutive fresh integer draws rounded to the same published value; the anchor must be
    // the raw value of the most recent fresh draw (306), not of the run's oldest entry (300).
    List<NoiseAnchors.HistoryEntry<Integer>> history =
        List.of(
            new NoiseAnchors.HistoryEntry<>(306, 302, null),
            new NoiseAnchors.HistoryEntry<>(300, 302, null));

    NoiseAnchors.PreviousNoise<Integer> noise = NoiseAnchors.reconstruct(history, THRESHOLD);

    assertThat(noise.published()).isEqualTo(302);
    assertThat(noise.anchor()).isEqualTo(306);
  }

  @Test
  void reconstruct_withSingleUseableEntry_usesItsRawAsAnchor() {
    List<NoiseAnchors.HistoryEntry<Double>> history = List.of(entry(306, 302.0), entry(300, 999.0));

    NoiseAnchors.PreviousNoise<Double> noise = NoiseAnchors.reconstruct(history, THRESHOLD);

    assertThat(noise.anchor()).isEqualTo(306);
    assertThat(noise.epsilon()).isEqualTo(0.5);
  }

  @Test
  void reconstruct_skipsNullRawAndPublishedWithoutBreakingTheRun() {
    List<NoiseAnchors.HistoryEntry<Double>> history =
        List.of(
            entry(310, 302.0),
            entry(null, 302.0),
            entry(308, null),
            entry(306, 302.0),
            entry(300, 999.0));

    NoiseAnchors.PreviousNoise<Double> noise = NoiseAnchors.reconstruct(history, THRESHOLD);

    assertThat(noise.published()).isEqualTo(302.0);
    assertThat(noise.anchor()).isEqualTo(306);
  }

  @Test
  void reconstruct_withEmptyOrUnusableHistory_returnsNull() {
    assertThat(NoiseAnchors.reconstruct(List.of(), THRESHOLD)).isNull();
    assertThat(NoiseAnchors.reconstruct(List.of(entry(null, 302.0), entry(306, null)), THRESHOLD))
        .isNull();
  }

  @Test
  void reconstruct_matchesStoredAnchorReferenceOnRandomHistories() {
    Random random = new Random(20261030L);
    for (int trial = 0; trial < 200; trial++) {
      int length = 1 + random.nextInt(120);
      List<NoiseAnchors.HistoryEntry<Integer>> newestFirst = new ArrayList<>();
      Integer anchor = null;
      Integer published = null;
      for (int i = 0; i < length; i++) {
        int raw = 30 + random.nextInt(140);
        if (anchor == null || Math.abs(raw - anchor) >= THRESHOLD) {
          anchor = raw;
          // Fresh draw: integer noise with frequent collisions and suppression below 50.
          int noisyRaw = raw + random.nextInt(9) - 4;
          published = noisyRaw < 50 ? 0 : noisyRaw;
        }
        if (random.nextInt(10) == 0) {
          newestFirst.addFirst(new NoiseAnchors.HistoryEntry<>(null, published, null));
        }
        newestFirst.addFirst(new NoiseAnchors.HistoryEntry<>(raw, published, 1.0));
      }

      NoiseAnchors.PreviousNoise<Integer> reconstructed =
          NoiseAnchors.reconstruct(newestFirst, THRESHOLD);

      assertThat(reconstructed.published())
          .as("published value at trial %d", trial)
          .isEqualTo(published);
      assertThat(reconstructed.anchor()).as("anchor at trial %d", trial).isEqualTo(anchor);
    }
  }
}
