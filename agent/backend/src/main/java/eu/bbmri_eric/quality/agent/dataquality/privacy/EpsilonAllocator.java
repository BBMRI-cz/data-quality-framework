package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.util.ArrayList;
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
   * preference. If the preferences reach or exceed the budget, they are ignored and everything
   * falls back to an equal split instead of letting preferred checks starve the others.
   *
   * @param totalEpsilon the epsilon budget available for all check results in one report
   * @param checkIds the check id of each result being released (in any order, duplicates allowed)
   * @param preferredBudgetByCheckId per-check preferred epsilon budgets; may be empty
   * @return the epsilon allocated to each result, aligned with {@code checkIds}
   */
  public static List<Double> allocate(
      double totalEpsilon, List<Long> checkIds, Map<Long, Double> preferredBudgetByCheckId) {
    List<Double> allocated = new ArrayList<>(checkIds.size());
    double totalPreferred =
        checkIds.stream().mapToDouble(id -> preferredBudgetByCheckId.getOrDefault(id, 0.0)).sum();
    if (totalPreferred >= totalEpsilon) {
      for (int i = 0; i < checkIds.size(); i++) {
        allocated.add(totalEpsilon / checkIds.size());
      }
      return allocated;
    }
    long noPreferenceCount =
        checkIds.stream().filter(id -> !preferredBudgetByCheckId.containsKey(id)).count();
    double baseEpsilon =
        noPreferenceCount > 0 ? (totalEpsilon - totalPreferred) / noPreferenceCount : 0;
    for (Long checkId : checkIds) {
      allocated.add(preferredBudgetByCheckId.getOrDefault(checkId, baseEpsilon));
    }
    return allocated;
  }
}
