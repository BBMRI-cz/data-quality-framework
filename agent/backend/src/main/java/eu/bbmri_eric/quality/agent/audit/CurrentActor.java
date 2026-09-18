package eu.bbmri_eric.quality.agent.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resolves the currently authenticated username for audit purposes. */
public final class CurrentActor {

  private CurrentActor() {}

  /**
   * @return the current authenticated username, or {@code null} if there is none (unauthenticated
   *     or system-initiated action)
   */
  public static String username() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    String name = authentication.getName();
    return "anonymousUser".equals(name) ? null : name;
  }
}
