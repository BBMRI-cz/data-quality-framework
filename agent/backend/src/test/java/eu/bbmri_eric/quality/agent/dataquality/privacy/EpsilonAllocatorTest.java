package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EpsilonAllocatorTest {

  @Test
  void allocate_withoutPreferences_splitsEqually() {
    List<Double> allocated = EpsilonAllocator.allocate(2.0, List.of(1L, 2L, 3L, 4L), Map.of());
    assertThat(allocated).containsExactly(0.5, 0.5, 0.5, 0.5);
  }

  @Test
  void allocate_withPreferencesWithinBudget_honorsPreferencesAndSharesRemainder() {
    List<Double> allocated = EpsilonAllocator.allocate(1.0, List.of(1L, 99L), Map.of(1L, 0.4));
    assertThat(allocated).containsExactly(0.4, 0.6);
  }

  @Test
  void allocate_withAllResultsPreferred_honorsThemExactly() {
    List<Double> allocated =
        EpsilonAllocator.allocate(2.0, List.of(1L, 2L), Map.of(1L, 0.8, 2L, 0.7));
    assertThat(allocated).containsExactly(0.8, 0.7);
  }

  @Test
  void allocate_withPreferencesExceedingBudget_fallsBackToEqualSplit() {
    List<Double> allocated =
        EpsilonAllocator.allocate(1.0, List.of(1L, 2L), Map.of(1L, 0.9, 2L, 0.9));
    assertThat(allocated).containsExactly(0.5, 0.5);
  }

  @Test
  void allocate_withPreferencesExactlyEqualToBudget_fallsBackToEqualSplit() {
    // Preferences that take up the whole share are ignored, just like exceeding ones; they must
    // not starve the remaining checks by leaving them an epsilon of 0.
    List<Double> allocated =
        EpsilonAllocator.allocate(1.0, List.of(1L, 2L, 99L), Map.of(1L, 0.5, 2L, 0.5));
    assertThat(allocated).containsExactly(1.0 / 3, 1.0 / 3, 1.0 / 3);
  }

  @Test
  void allocate_withEmptyInput_returnsEmpty() {
    assertThat(EpsilonAllocator.allocate(1.0, List.of(), Map.of())).isEmpty();
  }
}
