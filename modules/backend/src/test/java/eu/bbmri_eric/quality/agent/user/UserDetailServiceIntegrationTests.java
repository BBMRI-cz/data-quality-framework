package eu.bbmri_eric.quality.agent.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestUserSeedConfig.class)
@Transactional
class UserDetailServiceIntegrationTests {

  @Autowired private UserRepository userRepository;
  @Autowired private UserDetailService service;
  @Autowired private PasswordEncoder passwordEncoder;

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

  @Test
  void changePassword_AdminUser_SuccessfullyChangesPassword() {
    service.changePassword("admin", "pass", "newPassword123", "newPassword123");

    var updatedUser = userRepository.findByUsername("admin").orElseThrow();
    assertThat(passwordEncoder.matches("newPassword123", updatedUser.getPassword())).isTrue();
    assertThat(passwordEncoder.matches("pass", updatedUser.getPassword())).isFalse();
  }

  @Test
  void changePassword_AdminUser_ThrowsWhenCurrentPasswordIncorrect() {
    assertThatThrownBy(
            () ->
                service.changePassword(
                    "admin", "wrongPassword", "newPassword123", "newPassword123"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Current password is incorrect");
  }

  @Test
  void changePassword_AdminUser_ThrowsWhenPasswordsDoNotMatch() {
    assertThatThrownBy(
            () -> service.changePassword("admin", "pass", "newPassword123", "differentPassword"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("New password and confirmation do not match");
  }

  @Test
  void changePassword_AdminUser_ThrowsWhenNewPasswordTooShort() {
    assertThatThrownBy(() -> service.changePassword("admin", "pass", "short", "short"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("New password must be at least 8 characters long");
  }

  @Test
  void changePassword_NonExistentUser_ThrowsUserNotFound() {
    assertThatThrownBy(
            () -> service.changePassword("nonexistent", "pass", "newPassword123", "newPassword123"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found: nonexistent");
  }
}
