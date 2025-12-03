package eu.bbmri_eric.quality.agent.user.impl;

import eu.bbmri_eric.quality.agent.user.dto.CustomUserDetails;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service for fetching user details for Spring Security authentication. Implements
 * UserDetailsService interface for integration with Spring Security.
 */
@Service
class UserDetailService implements UserDetailsService {

  private static final Logger logger = LoggerFactory.getLogger(UserDetailService.class);

  private final UserRepository userRepository;

  UserDetailService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Objects.requireNonNull(username, "Username cannot be null");
    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  logger.warn("User not found during authentication: {}", username);
                  return new UsernameNotFoundException("User not found: " + username);
                });
    logger.debug("Successfully loaded user details for: {}", username);
    return new CustomUserDetails(user);
  }
}
