package eu.bbmri_eric.quality.agent.user;

import static org.springframework.security.core.userdetails.User.withUsername;

import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

/** Service for fetching user details */
@Service
public class UserDetailService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(UserDetailService.class);
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  UserDetailService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return withUsername(user.getUsername()).password(user.getPassword()).build();
  }

  public PasswordChangeResponse changePassword(
      Principal principal, PasswordChangeRequest request, BindingResult bindingResult) {
    if (principal == null) {
      return new PasswordChangeResponse(false, "Authentication required");
    }
    if (bindingResult != null && bindingResult.hasErrors()) {
      String errorMessage =
          bindingResult.getFieldErrors().stream()
              .map(error -> error.getDefaultMessage())
              .filter(java.util.Objects::nonNull)
              .findFirst()
              .orElse("Validation failed");
      return new PasswordChangeResponse(false, errorMessage);
    }
    String username = principal.getName();
    log.info("Password change attempt for user: {}", username);
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      log.warn("Password change failed for user {}: password confirmation mismatch", username);
      return new PasswordChangeResponse(false, "New password and confirmation do not match");
    }
    try {
      boolean success =
          changePassword(username, request.getCurrentPassword(), request.getNewPassword());
      if (success) {
        log.info("Password successfully changed for user: {}", username);
        return new PasswordChangeResponse(true, "Password changed successfully");
      } else {
        log.warn("Password change failed for user {}: incorrect current password", username);
        return new PasswordChangeResponse(false, "Current password is incorrect");
      }
    } catch (UsernameNotFoundException e) {
      log.error("User not found during password change: {}", username);
      return new PasswordChangeResponse(false, "User not found");
    } catch (Exception e) {
      log.error(
          "Unexpected error during password change for user {}: {}", username, e.getMessage(), e);
      return new PasswordChangeResponse(false, "An unexpected error occurred");
    }
  }

  private boolean changePassword(String username, String currentPassword, String newPassword) {
    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      return false;
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    return true;
  }
}
