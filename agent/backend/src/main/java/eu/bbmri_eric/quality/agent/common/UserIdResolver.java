package eu.bbmri_eric.quality.agent.common;

import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * Resolves the ID of the {@code User} behind a Spring Security {@link Authentication}.
 *
 * <p>Implemented by the {@code user} module, so other modules can obtain user IDs without depending
 * on its principal type. Combine with {@link CurrentUser#getAuthentication()} for the current user.
 */
public interface UserIdResolver {

  /**
   * @param authentication the authentication to resolve, never {@code null}
   * @return the ID of the {@code User} behind {@code authentication}, or empty if its principal is
   *     not a recognized user type
   */
  Optional<Long> resolveUserId(Authentication authentication);
}
