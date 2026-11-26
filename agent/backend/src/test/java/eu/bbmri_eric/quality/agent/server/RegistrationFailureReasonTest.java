package eu.bbmri_eric.quality.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegistrationFailureReasonTest {

  @Test
  void everyReason_hasNonBlankSummaryAndHint() {
    for (RegistrationFailureReason reason : RegistrationFailureReason.values()) {
      assertThat(reason.summary())
          .as("summary of %s", reason)
          .isNotBlank()
          .doesNotStartWith(" ")
          .doesNotEndWith(" ");
      assertThat(reason.hint())
          .as("hint of %s", reason)
          .isNotBlank()
          .doesNotStartWith(" ")
          .doesNotEndWith(" ");
    }
  }
}
