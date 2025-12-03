package eu.bbmri_eric.quality.agent.user.impl;

import eu.bbmri_eric.quality.agent.user.AuthenticationContextService;
import eu.bbmri_eric.quality.agent.user.UserService;
import eu.bbmri_eric.quality.agent.user.domain.User;
import eu.bbmri_eric.quality.agent.user.dto.PasswordChangeRequest;
import eu.bbmri_eric.quality.agent.user.dto.UserDTO;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for user management operations. Handles password changes for authenticated
 * users.
 */
@Service
class UserServiceImpl implements UserService {

  private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationContextService authenticationContextService;

  UserServiceImpl(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationContextService authenticationContextService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationContextService = authenticationContextService;
  }

  @Override
  @Transactional
  public void changePassword(Long userId, PasswordChangeRequest passwordChangeRequest) {
    Objects.requireNonNull(userId, "User ID cannot be null");
    Objects.requireNonNull(passwordChangeRequest, "Password change request cannot be null");
    passwordChangeRequest.validate();
    UserDTO currentUserDTO = authenticationContextService.getCurrentUser();
    String username = currentUserDTO.getUsername();
    Long currentUserId = currentUserDTO.getUserId();
    if (!currentUserId.equals(userId)) {
      logger.warn("User {} attempted to change password for user ID {}", username, userId);
      throw new AccessDeniedException("You can only change your own password");
    }
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  logger.warn("User not found: {}", username);
                  return new UsernameNotFoundException("User not found: " + username);
                });
    if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }
    String encodedPassword = passwordEncoder.encode(passwordChangeRequest.getNewPassword());
    user.setPassword(encodedPassword);
    logger.debug("Password successfully changed for user: {}", username);
  }
}
