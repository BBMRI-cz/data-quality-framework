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

  /**
   * Checks if there is a valid authenticated user in the current context.
   *
   * @return true if a user is authenticated, false otherwise
   */
  boolean isAuthenticated();
}
