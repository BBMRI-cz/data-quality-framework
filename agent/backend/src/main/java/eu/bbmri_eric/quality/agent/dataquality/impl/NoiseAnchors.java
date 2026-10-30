package eu.bbmri_eric.quality.agent.dataquality.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Derives the previous noise state of a published count from the report history — no extra state is
 * stored.
 *
 * <p>The anchor is the raw count that the current published value was drawn for. It is
 * reconstructed in two steps. First, the run of identical published values is collected by walking
 * the history newest-first until a different published value appears; entries with a null raw or
 * published value are skipped without breaking the run. Second, the refresh rule ({@code |raw -
 * anchor| >= refreshThreshold}) is replayed forward over that run, starting from the run's oldest
 * entry.
 *
 * <p>This is exact, not an approximation: the oldest entry of a run is always a fresh draw — its
 * predecessor published a different value and reuse copies values bit-for-bit. Every later entry
 * was decided by the same refresh rule that the replay applies step by step. The replay therefore
 * reproduces each reuse/refresh decision, including fresh draws that coincidentally published the
 * same value again (suppressed draws all publishing 0, or integer totals rounding to the same
 * number).
 */
final class NoiseAnchors {

  private NoiseAnchors() {}

  /** One history row for a single count, in newest-first order. */
  record HistoryEntry<T>(Integer raw, T published, Double epsilon) {}

  /**
   * The reconstructed noise state of a count: the currently published value, the replayed anchor it
   * was drawn for, and the epsilon used for it.
   */
  record PreviousNoise<T>(T published, Integer anchor, Double epsilon) {}

  /**
   * Reconstructs the previous noise state from the newest-first history of a single count.
   *
   * @param newestFirst history rows of one count, ordered newest first; entries with null raw or
   *     published values are skipped
   * @param refreshThreshold minimum drift from the anchor that triggers a fresh draw
   * @return the reconstructed state, or {@code null} when the history has no usable entry
   */
  static <T> PreviousNoise<T> reconstruct(List<HistoryEntry<T>> newestFirst, int refreshThreshold) {
    List<HistoryEntry<T>> usable = new ArrayList<>(newestFirst.size());
    for (HistoryEntry<T> entry : newestFirst) {
      if (entry.raw() != null && entry.published() != null) {
        usable.add(entry);
      }
    }
    if (usable.isEmpty()) {
      return null;
    }
    T chainValue = usable.getFirst().published();
    int runEnd = 0;
    while (runEnd + 1 < usable.size() && chainValue.equals(usable.get(runEnd + 1).published())) {
      runEnd++;
    }
    // replay the refresh rule forward, starting from the run's oldest (provably fresh) entry
    Integer anchor = usable.get(runEnd).raw();
    for (int i = runEnd - 1; i >= 0; i--) {
      int raw = usable.get(i).raw();
      if (Math.abs(raw - anchor) >= refreshThreshold) {
        anchor = raw;
      }
    }
    HistoryEntry<T> newest = usable.getFirst();
    return new PreviousNoise<>(newest.published(), anchor, newest.epsilon());
  }
}
