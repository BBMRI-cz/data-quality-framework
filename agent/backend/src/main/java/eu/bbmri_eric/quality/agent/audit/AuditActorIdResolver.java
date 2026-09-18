package eu.bbmri_eric.quality.agent.audit;

import org.springframework.security.core.Authentication;

/**
 * Resolves the ID of the {@code User} behind a Spring Security {@link Authentication}.
 *
 * <p>Implemented outside this module (by the {@code user} module) so that the audit module never
 * needs to know about a specific principal type, which would otherwise create a dependency cycle
 * ({@code user} already depends on {@code audit} for {@link Audited}).
 */
public interface AuditActorIdResolver {

  /**
   * @param authentication the current authentication, never {@code null}
   * @return the ID of the {@code User} behind {@code authentication}, or {@code null} if it can't
   *     be resolved (e.g. the principal isn't a recognized user type)
   */
  Long resolveActorId(Authentication authentication);
}
