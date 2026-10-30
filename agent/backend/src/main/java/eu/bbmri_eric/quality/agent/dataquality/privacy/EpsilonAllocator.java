package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.util.List;
import java.util.Map;

/**
 * Splits a report's check-results epsilon budget across results, honoring per-check budget
 * preferences. Pure function: no settings access, no randomness.
 */
public final class EpsilonAllocator {

  private EpsilonAllocator() {}

  /**
   * Allocates the budget to results, in the same order as {@code checkIds}. Results of checks with
   * a preferred budget get that budget; the remainder is split equally among the results without a
   * preference. If the preferences exceed the budget, everything falls back to an equal split
   * instead of letting preferred checks starve the others.
   *
   * @param totalEpsilon the epsilon budget available for all check results in one report
   * @param checkIds the check id of each result being released (in any order, duplicates allowed)
   * @param preferredBudgetByCheckId per-check preferred epsilon budgets; may be empty
   * @return the epsilon allocated to each result, aligned with {@code checkIds}
   * @throws IllegalArgumentException when the preferences leave no epsilon for results without a
   *     preference
   */
  public static double[] allocate(
      double totalEpsilon, List<Long> checkIds, Map<Long, Double> preferredBudgetByCheckId) {
    int count = checkIds.size();
    if (count == 0) {
      return new double[0];
    }
    double totalPreferred =
        checkIds.stream().mapToDouble(id -> preferredBudgetByCheckId.getOrDefault(id, 0.0)).sum();
    long noPreferenceCount =
        checkIds.stream().filter(id -> !preferredBudgetByCheckId.containsKey(id)).count();

    double[] allocated = new double[count];
    if (totalPreferred > totalEpsilon) {
      double equalShare = totalEpsilon / count;
      java.util.Arrays.fill(allocated, equalShare);
      return allocated;
    }
    double baseEpsilon = 0;
    if (noPreferenceCount > 0) {
      double remaining = totalEpsilon - totalPreferred;
      if (remaining <= 0) {
        throw new IllegalArgumentException(
            "Per-check epsilon budget preferences (%s) consume the entire epsilon budget available"
                + " for check results (%s), leaving no epsilon for %d result(s) without a"
                + " preference. Reduce the preferred budgets or increase the overall epsilon"
                + " budget.".formatted(totalPreferred, totalEpsilon, noPreferenceCount));
      }
      baseEpsilon = remaining / noPreferenceCount;
    }
    for (int i = 0; i < count; i++) {
      allocated[i] = preferredBudgetByCheckId.getOrDefault(checkIds.get(i), baseEpsilon);
    }
    return allocated;
  }
}
