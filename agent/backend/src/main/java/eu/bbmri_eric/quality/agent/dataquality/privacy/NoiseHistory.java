package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.util.ArrayList;
import java.util.List;

/**
 * Reconstructs the previous noise state of one published count from its history, and decides
 * whether that noise may be reused.
 *
 * <p>The anchor — the raw count the current published value was drawn for — is not stored. It is
 * reconstructed exactly: walk the newest-first history to the end of the run of identical published
 * values, then replay the refresh rule ({@code |raw - anchor| >= refreshThreshold}) forward over
 * that run. Entries with a null raw or published value are skipped without breaking the run. The
 * replay is exact because the run's oldest entry is provably a fresh draw (its predecessor
 * published a different value), and every later entry was decided by the same rule the replay
 * applies — including fresh draws that coincidentally published the same value again (suppressed
 * draws all publishing 0, or integer totals rounding to the same number).
 */
public final class NoiseHistory {

  private NoiseHistory() {}

  /**
   * One history row of a single published count, newest first. {@code published} is {@link
   * Double#NaN} when nothing was published for that entry (e.g. a missing check result);
   * unstructured entries with a null raw or {@code NaN} are skipped without breaking the run.
   */
  public record HistoryEntry(Integer raw, double published, Double epsilon) {}

  /** The reconstructed noise state of a count: last published value, its anchor, and epsilon. */
  public record PreviousRelease(double published, int anchor, Double epsilon) {

    /** The single reuse rule: drift below the threshold, and no republishing below suppression. */
    public boolean reusableFor(int raw, int refreshThreshold, double minThreshold) {
      return Math.abs(raw - anchor) < refreshThreshold
          && (published == 0.0 || published >= minThreshold);
    }
  }

  /**
   * Reconstructs the previous release from the newest-first history of one count.
   *
   * @return the reconstructed release, or {@code null} when the history has no usable entry
   */
  public static PreviousRelease replay(List<HistoryEntry> newestFirst, int refreshThreshold) {
    List<HistoryEntry> usable = new ArrayList<>(newestFirst.size());
    for (HistoryEntry entry : newestFirst) {
      if (entry.raw() != null && !Double.isNaN(entry.published())) {
        usable.add(entry);
      }
    }
    if (usable.isEmpty()) {
      return null;
    }
    double chainValue = usable.getFirst().published();
    int runEnd = 0;
    while (runEnd + 1 < usable.size() && usable.get(runEnd + 1).published() == chainValue) {
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
    return new PreviousRelease(newest.published(), anchor, newest.epsilon());
  }
}
