package eu.bbmri_eric.quality.agent.user.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.quality.agent.user.AuthenticationContextService;
import eu.bbmri_eric.quality.agent.user.UserService;
import eu.bbmri_eric.quality.agent.user.domain.User;
import eu.bbmri_eric.quality.agent.user.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
class UserDetailServiceTest {

  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";

  @Autowired private UserRepository userRepository;
  @Autowired private UserDetailService userDetailService;
  @Autowired private UserService userService;
  @Autowired private AuthenticationContextService authenticationContextService;
  @Autowired private PasswordEncoder passwordEncoder;

  @AfterEach
  void tearDown() {
    User adminUser = userRepository.findByUsername(ADMIN_USER).orElse(null);
    if (adminUser != null) {
      adminUser.setPassword(passwordEncoder.encode(ADMIN_PASS));
      userRepository.save(adminUser);
    }

    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
    userRepository.findByUsername("user1").ifPresent(userRepository::delete);
  }

  @Test
  void loadsUser_withROLE_USER_whenRoleIsUser() {
    userRepository.save(new User("user1", "pass1"));
    var details = userDetailService.loadUserByUsername("user1");
    assertThat(details.getUsername()).isEqualTo("user1");
    assertThat(details.getPassword()).isEqualTo("pass1");
  }

  @Test
  void throwsUsernameNotFound_whenUserMissing() {
    assertThatThrownBy(() -> userDetailService.loadUserByUsername("absent"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  @WithMockUser(username = ADMIN_USER)
  void getUserId_returnsCorrectId_whenUserExists() {
    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    Long expectedId = adminUser.getId();

    UserDTO userDTO = authenticationContextService.getCurrentUser();
    Long actualId = userDTO.getUserId();

    assertThat(actualId).isEqualTo(expectedId);
  }
}
