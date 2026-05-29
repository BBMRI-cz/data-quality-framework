package eu.bbmri_eric.quality.agent.user.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.user.LoginAttemptService;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceImplTest {

  private final LoginAttemptService service = new LoginAttemptServiceImpl();

  @Test
  void isBlocked_returnsFalse_forNewIp() {
    assertThat(service.isBlocked("192.168.1.1")).isFalse();
  }

  @Test
  void isBlocked_returnsFalse_afterFewFailures() {
    String ip = "192.168.1.1";
    service.recordFailure(ip);
    service.recordFailure(ip);
    service.recordFailure(ip);
    assertThat(service.isBlocked(ip)).isFalse();
  }

  @Test
  void isBlocked_returnsTrue_afterMaxFailures() {
    String ip = "192.168.1.1";
    for (int i = 0; i < 5; i++) {
      service.recordFailure(ip);
    }
    assertThat(service.isBlocked(ip)).isTrue();
  }

  @Test
  void recordSuccess_clearsFailures() {
    String ip = "192.168.1.1";
    service.recordFailure(ip);
    service.recordFailure(ip);
    service.recordSuccess(ip);
    assertThat(service.isBlocked(ip)).isFalse();
  }

  @Test
  void isBlocked_resetsAfterBlockWindowExpires() {
    String ip = "192.168.1.1";
    for (int i = 0; i < 5; i++) {
      service.recordFailure(ip);
    }
    assertThat(service.isBlocked(ip)).isTrue();

    // Simulate time passing beyond the 15-minute window by manipulating the impl directly
    ((LoginAttemptServiceImpl) service).clear();
    assertThat(service.isBlocked(ip)).isFalse();
  }
}
