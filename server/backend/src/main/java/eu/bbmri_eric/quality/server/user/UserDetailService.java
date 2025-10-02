package eu.bbmri_eric.quality.server.user;

import static org.springframework.security.core.userdetails.User.withUsername;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service for fetching user details for Spring Security authentication. Implements
 * UserDetailsService interface for integration with Spring Security.
 */
@Service
public class UserDetailService implements UserDetailsService {

  private static final Logger logger = LoggerFactory.getLogger(UserDetailService.class);

  private final UserRepository userRepository;

  UserDetailService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Objects.requireNonNull(username, "Username cannot be null");

    logger.debug("Loading user details for username: {}", username);

    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  logger.warn("User not found during authentication: {}", username);
                  return new UsernameNotFoundException("User not found: " + username);
                });

    logger.debug("Successfully loaded user details for: {}", username);
    return withUsername(user.getUsername()).password(user.getPassword()).build();
  }
}
