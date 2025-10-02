package eu.bbmri_eric.quality.agent.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class UserDetailServiceTest {

  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";

  @Autowired private UserRepository userRepository;

  @Autowired private UserDetailService service;

  @Autowired private PasswordEncoder passwordEncoder;

  @AfterEach
  void tearDown() {
    // Reset admin password to default state after each test
    User adminUser = userRepository.findByUsername(ADMIN_USER).orElse(null);
    if (adminUser != null) {
      adminUser.setPassword(passwordEncoder.encode(ADMIN_PASS));
      userRepository.save(adminUser);
    }

    // Clean up any test users that might have been created
    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
    userRepository.findByUsername("user1").ifPresent(userRepository::delete);
  }

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
  void changePassword_success() {
    PasswordChangeRequest request =
        new PasswordChangeRequest(ADMIN_PASS, "newAdminPass123!", "newAdminPass123!");

    boolean result = service.changePassword(ADMIN_USER, request);

    assertThat(result).isTrue();

    User updatedUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    assertThat(passwordEncoder.matches("newAdminPass123!", updatedUser.getPassword())).isTrue();
  }

  @Test
  void changePassword_failure_whenCurrentPasswordIncorrect() {
    PasswordChangeRequest request =
        new PasswordChangeRequest("wrongCurrentPass", "newPass123!", "newPass123!");

    assertThatThrownBy(() -> service.changePassword(ADMIN_USER, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Current password is incorrect");
  }

  @Test
  void changePassword_failure_whenPasswordsDoNotMatch() {
    PasswordChangeRequest request =
        new PasswordChangeRequest(ADMIN_PASS, "newPass123!", "differentPass");

    assertThatThrownBy(() -> service.changePassword(ADMIN_USER, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("New password and confirmation do not match");
  }

  @Test
  void changePassword_failure_whenUserNotFound() {
    PasswordChangeRequest request =
        new PasswordChangeRequest("currentPass", "newPass123!", "newPass123!");

    assertThatThrownBy(() -> service.changePassword("nonExistentUser", request))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found");
  }

  @Test
  void changePassword_failure_whenPasswordTooShort() {
    PasswordChangeRequest request = new PasswordChangeRequest(ADMIN_PASS, "short", "short");

    assertThatThrownBy(() -> service.changePassword(ADMIN_USER, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Password must be at least 8 characters long and contain only letters, digits or special characters");
  }

  @Test
  void changePassword_failure_whenPasswordInvalidCharacters() {
    PasswordChangeRequest request =
        new PasswordChangeRequest(ADMIN_PASS, "invalidчар123!", "invalidчар123!");

    assertThatThrownBy(() -> service.changePassword(ADMIN_USER, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Password must be at least 8 characters long and contain only letters, digits or special characters");
  }

  @Test
  void isUsingDefaultPassword_returnsTrue_whenAdminUsesDefaultPassword() {
    boolean result = service.isUsingDefaultPassword(ADMIN_USER);

    assertThat(result).isTrue();
  }

  @Test
  void isUsingDefaultPassword_returnsFalse_whenAdminPasswordChanged() {
    // Change password within test
    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    adminUser.setPassword(passwordEncoder.encode("newPassword123!"));
    userRepository.save(adminUser);

    boolean result = service.isUsingDefaultPassword(ADMIN_USER);
    assertThat(result).isFalse();
  }

  @Test
  void isUsingDefaultPassword_returnsFalse_whenNonAdminUser() {
    userRepository.save(new User("testuser", passwordEncoder.encode("somepassword123!")));
    boolean result = service.isUsingDefaultPassword("testuser");

    assertThat(result).isFalse();
  }

  @Test
  void passwordChangeRemovesDefaultPasswordFlag() {
    assertThat(service.isUsingDefaultPassword(ADMIN_USER)).isTrue();

    PasswordChangeRequest request =
        new PasswordChangeRequest(ADMIN_PASS, "newPassword123!", "newPassword123!");
    boolean result = service.changePassword(ADMIN_USER, request);

    assertThat(result).isTrue();
    assertThat(service.isUsingDefaultPassword(ADMIN_USER)).isFalse();
  }

  @Test
  void getUserId_returnsCorrectId_whenUserExists() {
    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    Long expectedId = adminUser.getId();

    Long actualId = service.getUserId(ADMIN_USER);

    assertThat(actualId).isEqualTo(expectedId);
  }

  @Test
  void getUserId_throwsException_whenUserNotFound() {
    assertThatThrownBy(() -> service.getUserId("nonExistentUser"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found");
  }
}
