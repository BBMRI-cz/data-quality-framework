package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NoiseHistoryTest {

  private static final int THRESHOLD = PrivacyBudget.NOISE_REFRESH_THRESHOLD;

  private static NoiseHistory.HistoryEntry entry(Integer raw, double published) {
    return new NoiseHistory.HistoryEntry(raw, published, 0.5);
  }

  @Test
  void replay_withSuppressedRun_replaysAnchorToLastFreshDraw() {
    // All fresh draws below the suppression threshold published 0.0; the anchor must replay
    // forward through the equal-value run instead of returning the oldest raw value (20).
    List<NoiseHistory.HistoryEntry> history =
        List.of(entry(27, 0.0), entry(25, 0.0), entry(22, 0.0), entry(20, 0.0));

    NoiseHistory.PreviousRelease previous = NoiseHistory.replay(history, THRESHOLD);

    assertThat(previous.published()).isEqualTo(0.0);
    assertThat(previous.anchor()).isEqualTo(25);
    assertThat(previous.epsilon()).isEqualTo(0.5);
  }

  @Test
  void replay_withRoundingCollision_replaysAnchorToLastFreshDraw() {
    // Two consecutive fresh draws rounded to the same published value; anchor is the most recent
    // fresh draw (306), not the run's oldest raw (300).
    List<NoiseHistory.HistoryEntry> history =
        List.of(
            new NoiseHistory.HistoryEntry(306, 302, null),
            new NoiseHistory.HistoryEntry(300, 302, null));

    NoiseHistory.PreviousRelease previous = NoiseHistory.replay(history, THRESHOLD);

    assertThat(previous.published()).isEqualTo(302.0);
    assertThat(previous.anchor()).isEqualTo(306);
  }

  @Test
  void replay_skipsNullRawAndUnpublishedEntriesWithoutBreakingTheRun() {
    List<NoiseHistory.HistoryEntry> history =
        List.of(
            entry(310, 302.0),
            entry(null, 302.0),
            entry(308, Double.NaN),
            entry(306, 302.0),
            entry(300, 999.0));

    NoiseHistory.PreviousRelease previous = NoiseHistory.replay(history, THRESHOLD);

    assertThat(previous.published()).isEqualTo(302.0);
    assertThat(previous.anchor()).isEqualTo(306);
  }

  @Test
  void replay_withEmptyOrUnusableHistory_returnsNull() {
    assertThat(NoiseHistory.replay(List.of(), THRESHOLD)).isNull();
    assertThat(NoiseHistory.replay(List.of(entry(null, 302.0), entry(306, Double.NaN)), THRESHOLD))
        .isNull();
  }

  @Test
  void replay_matchesStoredAnchorReferenceOnSeededRandomHistories() {
    Random random = new Random(20261030L);
    for (int trial = 0; trial < 200; trial++) {
      int length = 1 + random.nextInt(120);
      List<NoiseHistory.HistoryEntry> newestFirst = new ArrayList<>();
      Integer anchor = null;
      double published = Double.NaN;
      for (int i = 0; i < length; i++) {
        int raw = 30 + random.nextInt(140);
        if (anchor == null || Math.abs(raw - anchor) >= THRESHOLD) {
          anchor = raw;
          int noisyRaw = raw + random.nextInt(9) - 4;
          published = noisyRaw < 50 ? 0 : noisyRaw;
        }
        newestFirst.addFirst(new NoiseHistory.HistoryEntry(raw, published, 1.0));
      }

      NoiseHistory.PreviousRelease reconstructed = NoiseHistory.replay(newestFirst, THRESHOLD);

      assertThat(reconstructed.published()).as("published at trial %d", trial).isEqualTo(published);
      assertThat(reconstructed.anchor()).as("anchor at trial %d", trial).isEqualTo(anchor);
    }
  }

  @Test
  void reusableFor_acceptsSmallDriftAndRejectsLarge() {
    NoiseHistory.PreviousRelease previous = new NoiseHistory.PreviousRelease(95.5, 100, 0.5);
    assertThat(previous.reusableFor(104, 5, 50)).isTrue();
    assertThat(previous.reusableFor(105, 5, 50)).isFalse();
  }

  @Test
  void reusableFor_neverRepublishesBelowCurrentSuppressionThreshold_exceptZero() {
    NoiseHistory.PreviousRelease smallNonZero = new NoiseHistory.PreviousRelease(40.0, 40, 0.5);
    NoiseHistory.PreviousRelease suppressed = new NoiseHistory.PreviousRelease(0.0, 30, 0.5);
    assertThat(smallNonZero.reusableFor(40, 5, 50)).isFalse();
    assertThat(suppressed.reusableFor(30, 5, 50)).isTrue();
  }
}
