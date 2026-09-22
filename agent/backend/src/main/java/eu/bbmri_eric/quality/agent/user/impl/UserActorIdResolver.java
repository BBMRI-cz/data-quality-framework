package eu.bbmri_eric.quality.agent.user.impl;

import eu.bbmri_eric.quality.agent.audit.AuditActorIdResolver;
import eu.bbmri_eric.quality.agent.user.dto.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Resolves the current {@code User}'s ID from a {@link CustomUserDetails} principal. */
@Component
class UserActorIdResolver implements AuditActorIdResolver {

  @Override
  public Long resolveActorId(Authentication authentication) {
    return authentication.getPrincipal() instanceof CustomUserDetails userDetails
        ? userDetails.getUser().getUserId()
        : null;
  }
}