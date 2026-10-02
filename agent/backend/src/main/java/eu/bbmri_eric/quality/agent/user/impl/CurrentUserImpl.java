package eu.bbmri_eric.quality.agent.user.impl;

import eu.bbmri_eric.quality.agent.common.CurrentUser;
import eu.bbmri_eric.quality.agent.user.dto.CustomUserDetails;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the currently authenticated user from the Spring Security context. */
@Component
class CurrentUserImpl implements CurrentUser {

  @Override
  public Optional<String> getUsername() {
    return getAuthentication().map(Authentication::getName);
  }

  @Override
  public Optional<Long> getUserId() {
    return getAuthentication()
        .map(Authentication::getPrincipal)
        .filter(CustomUserDetails.class::isInstance)
        .map(CustomUserDetails.class::cast)
        .map(userDetails -> userDetails.getUser().getUserId());
  }

  private Optional<Authentication> getAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getName())) {
      return Optional.empty();
    }
    return Optional.of(authentication);
  }
}
