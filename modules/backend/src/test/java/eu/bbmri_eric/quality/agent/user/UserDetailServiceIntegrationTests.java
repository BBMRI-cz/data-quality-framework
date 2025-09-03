package eu.bbmri_eric.quality.agent.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@SpringBootTest
class UserDetailServiceIntegrationTests {

  @Autowired private UserRepository userRepository;

  @Autowired private UserDetailService service;

  @Test
  void loadsUser_withROLE_USER_whenRoleIsUser() {
    userRepository.save(new User("user1", "pass1"));

    var details = service.loadUserByUsername("user1");

    assertThat(details.getUsername()).isEqualTo("user1");
    assertThat(details.getPassword()).isEqualTo("pass1");
  }

  @Test
  void throwsUsernameNotFound_whenUserMissing() {
    assertThatThrownBy(() -> service.loadUserByUsername("absent"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
