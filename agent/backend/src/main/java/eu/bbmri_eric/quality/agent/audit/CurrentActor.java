package eu.bbmri_eric.quality.agent.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resolves the currently authenticated user for audit purposes. */
public final class CurrentActor {

  private CurrentActor() {}

  /**
   * @return the current authenticated username, or {@code null} if there is none (unauthenticated
   *     or system-initiated action)
   */
  public static String username() {
    Authentication authentication = authentication();
    return authentication == null ? null : authentication.getName();
  }

  /**
   * @return the current {@link Authentication}, or {@code null} if there is none (unauthenticated,
   *     anonymous, or system-initiated action). Useful together with {@link AuditActorIdResolver}
   *     to resolve the actor's ID without this module depending on a specific principal type.
   */
  public static Authentication authentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    return "anonymousUser".equals(authentication.getName()) ? null : authentication;
  }
}
