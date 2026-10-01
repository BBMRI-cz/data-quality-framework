package eu.bbmri_eric.quality.agent.common;

import java.util.Optional;

/**
 * Provides access to the currently authenticated user from the Spring Security context.
 *
 * <p>Implemented by the {@code user} module, so other modules can obtain the current user without
 * depending on its principal type.
 */
public interface CurrentUser {

  /**
   * @return the username of the currently authenticated user, or empty if there is none
   *     (unauthenticated, anonymous, or system-initiated action)
   */
  Optional<String> getUsername();

  /**
   * @return the ID of the {@code User} behind the current authentication, or empty if there is no
   *     authenticated user or its principal is not a recognized user type
   */
  Optional<Long> getUserId();
}
