package eu.bbmri_eric.quality.agent.user.impl;

import eu.bbmri_eric.quality.agent.common.UserIdResolver;
import eu.bbmri_eric.quality.agent.user.dto.CustomUserDetails;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Resolves the {@code User} ID from a {@link CustomUserDetails} principal. */
@Component
class UserIdResolverImpl implements UserIdResolver {

  @Override
  public Optional<Long> resolveUserId(Authentication authentication) {
    return authentication.getPrincipal() instanceof CustomUserDetails userDetails
        ? Optional.ofNullable(userDetails.getUser().getUserId())
        : Optional.empty();
  }
}
