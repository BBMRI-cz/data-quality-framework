package eu.bbmri_eric.quality.agent.user;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UserDetailServiceTest {

  private UserRepository userRepository;
  private UserDetailService service;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    service = new UserDetailService(userRepository);
  }

  @Test
  void returnsUserWithROLE_USERWhenRoleIsUser() {
    var user = new User("user1", "pass1", "USER");
    when(userRepository.findByUsername("user1")).thenReturn(of(user));

    var details = service.loadUserByUsername("user1");

    assertThat(details.getUsername()).isEqualTo("user1");
    assertThat(details.getPassword()).isEqualTo("pass1");
    assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
  }

  @Test
  void defaultsNullRoleToROLE_USER() {
    var user = new User("user4", "pass4", null);
    when(userRepository.findByUsername("user4")).thenReturn(of(user));

    var details = service.loadUserByUsername("user4");

    assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
  }

  @Test
  void throwsWhenUserNotFound() {
    when(userRepository.findByUsername("user5")).thenReturn(empty());

    assertThatThrownBy(() -> service.loadUserByUsername("user5"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
