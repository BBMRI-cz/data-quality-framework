package eu.bbmri_eric.quality.agent.dataquality.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EpsilonAllocatorTest {

  @Test
  void allocate_withoutPreferences_splitsEqually() {
    double[] allocated = EpsilonAllocator.allocate(2.0, List.of(1L, 2L, 3L, 4L), Map.of());
    assertThat(allocated).containsExactly(0.5, 0.5, 0.5, 0.5);
  }

  @Test
  void allocate_withPreferencesWithinBudget_honorsPreferencesAndSharesRemainder() {
    double[] allocated = EpsilonAllocator.allocate(1.0, List.of(1L, 99L), Map.of(1L, 0.4));
    assertThat(allocated).containsExactly(0.4, 0.6);
  }

  @Test
  void allocate_withAllResultsPreferred_honorsThemExactly() {
    double[] allocated = EpsilonAllocator.allocate(2.0, List.of(1L, 2L), Map.of(1L, 0.8, 2L, 0.7));
    assertThat(allocated).containsExactly(0.8, 0.7);
  }

  @Test
  void allocate_withPreferencesExceedingBudget_fallsBackToEqualSplit() {
    double[] allocated = EpsilonAllocator.allocate(1.0, List.of(1L, 2L), Map.of(1L, 0.9, 2L, 0.9));
    assertThat(allocated).containsExactly(0.5, 0.5);
  }

  @Test
  void allocate_withPreferencesStarvingOthers_throws() {
    assertThatThrownBy(() -> EpsilonAllocator.allocate(1.0, List.of(1L, 99L), Map.of(1L, 1.0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("leaving no epsilon");
  }

  @Test
  void allocate_withEmptyInput_returnsEmpty() {
    assertThat(EpsilonAllocator.allocate(1.0, List.of(), Map.of())).isEmpty();
  }
}
