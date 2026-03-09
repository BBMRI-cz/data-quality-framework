package eu.bbmri_eric.quality.agent.settings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NoiseMechanismTest {

  @Test
  void fromString_withLaplace_shouldReturnLaplaceMechanism() {
    NoiseMechanism result = NoiseMechanism.fromString("LAPLACE");
    assertEquals(NoiseMechanism.LAPLACE, result);
  }

  @Test
  void fromString_withInvalidValue_shouldThrowException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> NoiseMechanism.fromString("INVALID"));

    assertTrue(exception.getMessage().contains("Unknown noise mechanism"));
    assertTrue(exception.getMessage().contains("INVALID"));
  }

  @Test
  void fromString_withNull_shouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> NoiseMechanism.fromString(null));
  }

  @Test
  void fromString_withEmptyString_shouldThrowException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> NoiseMechanism.fromString(""));

    assertTrue(exception.getMessage().contains("Unknown noise mechanism"));
  }

  @Test
  void getValue_forLaplace_shouldReturnLaplaceString() {
    String result = NoiseMechanism.LAPLACE.getValue();
    assertEquals("LAPLACE", result);
  }

  @Test
  void allValues_shouldHaveTwoMechanisms() {
    NoiseMechanism[] mechanisms = NoiseMechanism.values();
    assertEquals(2, mechanisms.length);
  }

  @Test
  void valueOf_withLaplace_shouldReturnLaplaceMechanism() {
    NoiseMechanism result = NoiseMechanism.valueOf("LAPLACE");
    assertEquals(NoiseMechanism.LAPLACE, result);
  }
}

