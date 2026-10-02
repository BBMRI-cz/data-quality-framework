package eu.bbmri_eric.quality.agent.user.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.Audited;
import eu.bbmri_eric.quality.agent.common.CurrentUser;
import eu.bbmri_eric.quality.agent.common.JwtUtil;
import eu.bbmri_eric.quality.agent.user.AuthenticationContextService;
import eu.bbmri_eric.quality.agent.user.domain.User;
import eu.bbmri_eric.quality.agent.user.dto.CustomUserDetails;
import eu.bbmri_eric.quality.agent.user.dto.LoginRequest;
import eu.bbmri_eric.quality.agent.user.dto.LoginResponse;
import eu.bbmri_eric.quality.agent.user.dto.UserDTO;
import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implementation of AuthenticationContextService for handling authentication context operations.
 * Centralizes authentication-related logic and user retrieval.
 */
@Component
class AuthenticationContextServiceImpl implements AuthenticationContextService {

  private static final Logger logger =
      LoggerFactory.getLogger(AuthenticationContextServiceImpl.class);

  private final UserRepository userRepository;
  private final ModelMapper modelMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final CurrentUser currentUser;

  AuthenticationContextServiceImpl(
      UserRepository userRepository,
      ModelMapper modelMapper,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtUtil jwtUtil,
      CurrentUser currentUser) {
    this.userRepository = userRepository;
    this.modelMapper = modelMapper;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtUtil = jwtUtil;
    this.currentUser = currentUser;
  }

  @Override
  @Audited(
      action = AuditAction.LOGIN_SUCCESS,
      module = "user",
      entityId = "#result.user().userId",
      details = "Logged in")
  public LoginResponse login(LoginRequest loginRequest) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.username(), loginRequest.password()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    return new LoginResponse(jwtUtil.generateToken(authentication), userDetails.getUser());
  }

  @Override
  @Audited(
      action = AuditAction.LOGOUT,
      module = "user",
      entityId = "#result",
      details = "Logged out")
  public Long logout() {
    return currentUser
        .getUserId()
        .orElseThrow(
            () -> new AuthenticationCredentialsNotFoundException("No valid authentication found"));
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
    UserDTO userDTO = modelMapper.map(user, UserDTO.class);
    userDTO.setDefaultPassword(isUsingDefaultPassword(username, user));
    return userDTO;
  }

  private boolean isUsingDefaultPassword(String username, User user) {
    String defaultPassword = "admin".equals(username) ? "adminpass" : null;
    if (defaultPassword == null) {
      return false;
    }
    return passwordEncoder.matches(defaultPassword, user.getPassword());
  }
}
