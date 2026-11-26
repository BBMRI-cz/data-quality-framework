package eu.bbmri_eric.quality.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

class RegistrationFailureReasonTest {

  @Test
  void summaryAndHint_everyReason_areNotBlank() {
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

  @ParameterizedTest(name = "fromHttpStatus_{0}_yields{1}")
  @CsvSource({
    "401, UNAUTHORIZED",
    "403, UNAUTHORIZED",
    "409, CONFLICT",
    "429, RATE_LIMITED",
    "400, CLIENT_ERROR",
    "404, CLIENT_ERROR",
    "500, SERVER_ERROR",
    "503, SERVER_ERROR",
    "302, UNKNOWN",
  })
  void fromHttpStatus_status_yieldsMatchingReason(int status, RegistrationFailureReason expected) {
    assertThat(RegistrationFailureReason.fromHttpStatus(HttpStatus.valueOf(status)))
        .isEqualTo(expected);
  }

  @ParameterizedTest(name = "isRetryable_{0}_is_{1}")
  @CsvSource({
    "UNREACHABLE, true",
    "TIMEOUT, true",
    "TLS, true",
    "SERVER_ERROR, true",
    "RATE_LIMITED, true",
    "UNAUTHORIZED, false",
    "CONFLICT, false",
    "CLIENT_ERROR, false",
    "INCOMPLETE_RESPONSE, false",
    "UNKNOWN, false",
  })
  void isRetryable_reason_matchesRetryPolicy(RegistrationFailureReason reason, boolean expected) {
    assertThat(reason.isRetryable()).isEqualTo(expected);
  }
}
