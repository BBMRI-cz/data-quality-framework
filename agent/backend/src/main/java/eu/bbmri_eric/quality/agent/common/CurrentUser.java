package eu.bbmri_eric.quality.agent.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Provides access to the currently authenticated user from the Spring Security context. */
public final class CurrentUser {

  private CurrentUser() {}

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
   *     anonymous, or system-initiated action)
   */
  public static Authentication authentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    return "anonymousUser".equals(authentication.getName()) ? null : authentication;
  }
}
