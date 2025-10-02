package eu.bbmri_eric.quality.server.user;

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
}
