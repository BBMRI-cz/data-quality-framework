package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reconstructs the previous noise state of one published count from its history, so repeated
 * reports can reuse it instead of re-sampling.
 *
 * <p>The anchor — the raw count the current published value was drawn for — is not stored. It is
 * replayed exactly in two steps: walk the newest-first history to the end of the run of identical
 * published values, then re-apply the refresh rule ({@code |raw - anchor| >= refreshThreshold})
 * forward over that run. Entries with a null raw or published value are skipped without breaking
 * the run. The replay is exact because the run's oldest entry is provably a fresh draw (its
 * predecessor published a different value), and every later entry was decided by the same rule the
 * replay applies — including fresh draws that coincidentally published the same value again
 * (suppressed draws all publishing 0, or integer totals rounding to the same number).
 */
public final class NoiseHistory {

  private NoiseHistory() {}

  /**
   * Reconstructs the previous release from the newest-first history of one count, or {@link
   * Optional#empty()} when the history has no usable entry.
   */
  public static Optional<PreviousRelease> replay(
      List<HistoryEntry> newestFirst, int refreshThreshold) {
    List<HistoryEntry> usable = new ArrayList<>(newestFirst.size());
    for (HistoryEntry entry : newestFirst) {
      if (entry.raw() != null && entry.published() != null) {
        usable.add(entry);
      }
    }
    if (usable.isEmpty()) {
      return Optional.empty();
    }
    double chainValue = usable.getFirst().published();
    int runEnd = 0;
    // Double.compare (not ==) on purpose: equality here is about propagating the same published
    // value bit-for-bit, and it must also treat -0.0 and 0.0 differently just like the stored data.
    while (runEnd + 1 < usable.size()
        && Double.compare(usable.get(runEnd + 1).published(), chainValue) == 0) {
      runEnd++;
    }
    int anchor = usable.get(runEnd).raw();
    for (int i = runEnd - 1; i >= 0; i--) {
      int raw = usable.get(i).raw();
      if (Math.abs(raw - anchor) >= refreshThreshold) {
        anchor = raw;
      }
    }
    HistoryEntry newest = usable.getFirst();
    return Optional.of(new PreviousRelease(newest.published(), anchor, newest.epsilon()));
  }
}
