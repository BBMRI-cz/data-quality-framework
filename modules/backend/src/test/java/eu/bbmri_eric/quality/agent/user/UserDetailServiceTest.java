package eu.bbmri_eric.quality.agent.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@SpringBootTest
class UserDetailServiceTest {

  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";

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
  void changePassword_success() {
    Principal principal = () -> ADMIN_USER;
    PasswordChangeRequest request =
        new PasswordChangeRequest(ADMIN_PASS, "newAdminPass123", "newAdminPass123");

    PasswordChangeResponse response = service.changePassword(principal, request, null);

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMessage()).isEqualTo("Password changed successfully");

    User updatedUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    assertThat(passwordEncoder.matches("newAdminPass123", updatedUser.getPassword())).isTrue();

    updatedUser.setPassword(passwordEncoder.encode(ADMIN_PASS));
    userRepository.save(updatedUser);
  }

  @Test
  void changePassword_failure_whenCurrentPasswordIncorrect() {
    Principal principal = () -> ADMIN_USER;
    PasswordChangeRequest request =
        new PasswordChangeRequest("wrongCurrentPass", "newPass123_", "newPass123_");

    PasswordChangeResponse response = service.changePassword(principal, request, null);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getMessage()).isEqualTo("Current password is incorrect");
  }

  @Test
  void changePassword_failure_whenPasswordsDoNotMatch() {
    Principal principal = () -> ADMIN_USER;
    PasswordChangeRequest request =
        new PasswordChangeRequest(ADMIN_PASS, "newPass123_", "differentPass");

    PasswordChangeResponse response = service.changePassword(principal, request, null);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getMessage()).isEqualTo("New password and confirmation do not match");
  }

  @Test
  void changePassword_failure_whenPrincipalIsNull() {
    PasswordChangeRequest request =
        new PasswordChangeRequest("currentPass", "newPass123_", "newPass123_");

    PasswordChangeResponse response = service.changePassword(null, request, null);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getMessage()).isEqualTo("Authentication required");
  }

  @Test
  void changePassword_failure_whenUserNotFound() {
    Principal principal = () -> "nonExistentUser";
    PasswordChangeRequest request =
        new PasswordChangeRequest("currentPass", "newPass123_", "newPass123_");

    PasswordChangeResponse response = service.changePassword(principal, request, null);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getMessage()).isEqualTo("User not found");
  }

  @Test
  void changePassword_failure_whenValidationErrors() {
    Principal principal = () -> ADMIN_USER;
    PasswordChangeRequest request = new PasswordChangeRequest(ADMIN_PASS, "pass", "pass");

    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError =
        new FieldError(
            "passwordChangeRequest",
            "newPassword",
            "Password must be at least 8 characters long and contain only letters, digits, or basic special characters");

    when(bindingResult.hasErrors()).thenReturn(true);
    when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

    PasswordChangeResponse response = service.changePassword(principal, request, bindingResult);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getMessage())
        .isEqualTo(
            "Password must be at least 8 characters long and contain only letters, digits, or basic special characters");
  }

  @Test
  void changePassword_failure_whenValidationErrorsWithNoMessage() {
    Principal principal = () -> ADMIN_USER;
    PasswordChangeRequest request = new PasswordChangeRequest(ADMIN_PASS, "invalid", "invalid");

    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("passwordChangeRequest", "newPassword", null);

    when(bindingResult.hasErrors()).thenReturn(true);
    when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

    PasswordChangeResponse response = service.changePassword(principal, request, bindingResult);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getMessage()).isEqualTo("Validation failed");
  }
}
