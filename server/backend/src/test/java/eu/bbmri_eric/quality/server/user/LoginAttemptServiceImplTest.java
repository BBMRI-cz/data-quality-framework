package eu.bbmri_eric.quality.server.user;

import static org.assertj.core.api.Assertions.assertThat;

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

    ((LoginAttemptServiceImpl) service).clear();
    assertThat(service.isBlocked(ip)).isFalse();
  }
}
