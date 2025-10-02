package eu.bbmri_eric.quality.server.user;

import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Implementation of AuthenticationContextService for handling authentication context operations.
 * Centralizes authentication-related logic and user retrieval.
 */
@Component
public class AuthenticationContextServiceImpl implements AuthenticationContextService {

  private static final Logger logger =
      LoggerFactory.getLogger(AuthenticationContextServiceImpl.class);

  private final UserRepository userRepository;
  private final ModelMapper modelMapper;

  AuthenticationContextServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
    this.userRepository = userRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public UserDTO getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (Objects.isNull(authentication) || !authentication.isAuthenticated()) {
      logger.warn("Attempt to access authentication context without valid credentials");
      throw new AuthenticationCredentialsNotFoundException("No valid authentication found");
    }
    String username = authentication.getName();
    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  logger.warn("User not found: {}", username);
                  return new UsernameNotFoundException("User not found: " + username);
                });

    logger.debug("Successfully found user: {}", username);
    return modelMapper.map(user, UserDTO.class);
  }
}
