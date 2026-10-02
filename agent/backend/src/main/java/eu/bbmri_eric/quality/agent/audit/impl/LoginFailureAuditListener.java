package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

/**
 * Records failed logins, published by the {@code AuthenticationManager} (see {@code
 * SecurityConfig#authenticationEventPublisher}). Successful logins and logouts are recorded via
 * {@code @Audited} instead, which can't see a login that fails by throwing.
 */
@Component
class LoginFailureAuditListener {

  private final AuditRecorder auditRecorder;

  LoginFailureAuditListener(AuditRecorder auditRecorder) {
    this.auditRecorder = auditRecorder;
  }

  @EventListener
  void onLoginFailure(AbstractAuthenticationFailureEvent event) {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_FAILURE)
            .actor(event.getAuthentication().getName(), null)
            .module("user")
            .details(event.getException().getMessage())
            .build());
  }
}
