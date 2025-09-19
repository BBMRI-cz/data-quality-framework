package eu.bbmri_eric.quality.agent.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailService implements UserDetailsService {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  public UserDetailService(UserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var u =
        users
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
        .password(u.getPassword())
        .build();
  }

  @Transactional
  public void changePassword(
      String username, String currentPassword, String newPassword, String confirmPassword) {
    if (currentPassword == null || currentPassword.trim().isEmpty()) {
      throw new IllegalArgumentException("Current password is required");
    }

    if (newPassword == null || newPassword.length() < 8) {
      throw new IllegalArgumentException("New password must be at least 8 characters long");
    }

    if (!newPassword.equals(confirmPassword)) {
      throw new IllegalArgumentException("New password and confirmation do not match");
    }

    User user =
        users
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }

    if (passwordEncoder.matches(newPassword, user.getPassword())) {
      throw new IllegalArgumentException("New password must be different from current password");
    }

    String encodedNewPassword = passwordEncoder.encode(newPassword);
    user.setPassword(encodedNewPassword);

    users.save(user);
  }
}
