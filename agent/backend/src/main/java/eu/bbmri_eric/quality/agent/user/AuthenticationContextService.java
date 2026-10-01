package eu.bbmri_eric.quality.agent.user;

import eu.bbmri_eric.quality.agent.user.dto.LoginRequest;
import eu.bbmri_eric.quality.agent.user.dto.LoginResponse;
import eu.bbmri_eric.quality.agent.user.dto.UserDTO;

/**
 * Service interface for authentication context operations. Provides abstraction for
 * authentication-related functionality.
 */
public interface AuthenticationContextService {

  /**
   * Gets the current authenticated user's complete profile information.
   *
   * @return the UserDTO of the currently authenticated user
   * @throws org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
   *     if no valid authentication is found
   */
  UserDTO getCurrentUser();

  /**
   * Authenticates the given credentials and issues a JWT token for them.
   *
   * @param loginRequest the username and password
   * @return the JWT token and the authenticated user
   * @throws org.springframework.security.core.AuthenticationException if the credentials are
   *     invalid
   */
  LoginResponse login(LoginRequest loginRequest);

  /**
   * Logs out the currently authenticated user. Tokens are stateless, so this only records the
   * logout; the client discards the token.
   *
   * @return the ID of the user who logged out
   */
  Long logout();
}
