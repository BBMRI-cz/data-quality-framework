package eu.bbmri_eric.quality.agent.user;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserDetailServiceTest {

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserDetailService service;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    service = new UserDetailService(userRepository, passwordEncoder);
  }

  @Test
  void returnsUserWithROLE_USERWhenRoleIsUser() {
    var user = new User("user1", "pass1");
    when(userRepository.findByUsername("user1")).thenReturn(of(user));

    var details = service.loadUserByUsername("user1");

    assertThat(details.getUsername()).isEqualTo("user1");
    assertThat(details.getPassword()).isEqualTo("pass1");
  }

  @Test
  void throwsWhenUserNotFound() {
    when(userRepository.findByUsername("user5")).thenReturn(empty());

    assertThatThrownBy(() -> service.loadUserByUsername("user5"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  void changePassword_SuccessfullyChangesPassword() {
    var user = new User("testuser", "encodedOldPassword");
    when(userRepository.findByUsername("testuser")).thenReturn(of(user));
    when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
    when(passwordEncoder.matches("newPassword123", "encodedOldPassword")).thenReturn(false);
    when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

    service.changePassword("testuser", "oldPassword", "newPassword123", "newPassword123");

    verify(userRepository).save(user);
    assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
  }

  @Test
  void changePassword_ThrowsWhenCurrentPasswordIsNull() {
    assertThatThrownBy(
            () -> service.changePassword("testuser", null, "newPassword123", "newPassword123"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Current password is required");
  }

  @Test
  void changePassword_ThrowsWhenCurrentPasswordIsEmpty() {
    assertThatThrownBy(
            () -> service.changePassword("testuser", "  ", "newPassword123", "newPassword123"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Current password is required");
  }

  @Test
  void changePassword_ThrowsWhenNewPasswordIsNull() {
    assertThatThrownBy(
            () -> service.changePassword("testuser", "oldPassword", null, "newPassword123"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("New password must be at least 8 characters long");
  }

  @Test
  void changePassword_ThrowsWhenNewPasswordIsTooShort() {
    assertThatThrownBy(() -> service.changePassword("testuser", "oldPassword", "short", "short"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("New password must be at least 8 characters long");
  }

  @Test
  void changePassword_ThrowsWhenConfirmPasswordIsNull() {
    assertThatThrownBy(
            () -> service.changePassword("testuser", "oldPassword", "newPassword123", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("New password and confirmation do not match");
  }

  @Test
  void changePassword_ThrowsWhenConfirmPasswordDoesNotMatch() {
    assertThatThrownBy(
            () ->
                service.changePassword(
                    "testuser", "oldPassword", "newPassword123", "differentPassword"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("New password and confirmation do not match");
  }

  @Test
  void changePassword_ThrowsWhenUserNotFound() {
    when(userRepository.findByUsername("nonexistent")).thenReturn(empty());

    assertThatThrownBy(
            () ->
                service.changePassword(
                    "nonexistent", "oldPassword", "newPassword123", "newPassword123"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found: nonexistent");

    verify(userRepository, never()).save(any());
  }

  @Test
  void changePassword_ThrowsWhenCurrentPasswordIsIncorrect() {
    var user = new User("testuser", "encodedOldPassword");
    when(userRepository.findByUsername("testuser")).thenReturn(of(user));
    when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.changePassword(
                    "testuser", "wrongPassword", "newPassword123", "newPassword123"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Current password is incorrect");

    verify(userRepository, never()).save(any());
  }

  @Test
  void changePassword_ThrowsWhenNewPasswordIsSameAsCurrent() {
    var user = new User("testuser", "encodedPassword");
    when(userRepository.findByUsername("testuser")).thenReturn(of(user));
    when(passwordEncoder.matches("samePassword", "encodedPassword")).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.changePassword("testuser", "samePassword", "samePassword", "samePassword"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("New password must be different from current password");

    verify(userRepository, never()).save(any());
  }

  @Test
  void changePassword_DoesNotSaveWhenValidationFails() {
    assertThatThrownBy(() -> service.changePassword("testuser", "oldPassword", "short", "short"))
        .isInstanceOf(IllegalArgumentException.class);

    verify(userRepository, never()).save(any());
    verify(passwordEncoder, never()).encode(anyString());
  }
}
