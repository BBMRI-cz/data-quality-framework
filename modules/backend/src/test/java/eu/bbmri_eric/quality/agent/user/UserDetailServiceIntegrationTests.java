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
    userRepository.save(new User("user1", "pass1", "USER"));

    var details = service.loadUserByUsername("user1");

    assertThat(details.getUsername()).isEqualTo("user1");
    assertThat(details.getPassword()).isEqualTo("pass1");
    assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
  }

  @Test
  void loadsUser_withROLE_ADMIN_whenRoleIsAdmin() {
    userRepository.save(new User("user2", "pass2", "ADMIN"));

    var details = service.loadUserByUsername("user2");

    assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
  }

  @Test
  void defaultsBlankRole_toROLE_USER() {
    userRepository.save(new User("user3", "pass3", ""));

    var details = service.loadUserByUsername("user3");

    assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
  }

  @Test
  void defaultsNullRole_toROLE_USER() {
    userRepository.save(new User("user4", "pass4", null));

    var details = service.loadUserByUsername("user4");

    assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
  }

  @Test
  void throwsUsernameNotFound_whenUserMissing() {
    assertThatThrownBy(() -> service.loadUserByUsername("absent"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
