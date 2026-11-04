package eu.bbmri_eric.quality.server.user.impl;

import eu.bbmri_eric.quality.server.user.AuthenticationContextService;
import eu.bbmri_eric.quality.server.user.domain.User;
import eu.bbmri_eric.quality.server.user.dto.CustomUserDetails;
import eu.bbmri_eric.quality.server.user.dto.UserDTO;
import java.util.Objects;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    String name = authentication.getName();
    var user =
        findUser(authentication)
            .orElseThrow(
                () -> {
                  logger.warn("User not found: {}", name);
                  return new UsernameNotFoundException("User not found: " + name);
                });

    logger.debug("Successfully found user: {}", name);
    return modelMapper.map(user, UserDTO.class);
  }

  /**
   * Resolves the user by the stable identity established during authentication: the user ID for
   * internal tokens and the subject ID for OIDC tokens. Display usernames are not unique and are
   * only used as a fallback for other authentication types.
   */
  private Optional<User> findUser(Authentication authentication) {
    if (authentication.getPrincipal() instanceof CustomUserDetails details) {
      return userRepository.findById(details.getUser().getId());
    }
    if (authentication instanceof JwtAuthenticationToken) {
      return userRepository.findBySubjectId(authentication.getName());
    }
    return userRepository.findByUsername(authentication.getName());
  }
}
