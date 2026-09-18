package eu.bbmri_eric.quality.agent.audit;

import org.springframework.boot.actuate.security.AuthenticationAuditListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Spring Security's authentication events into the audit trail.
 *
 * <p>{@link AuthenticationAuditListener} converts the {@code AuthenticationSuccessEvent}/{@code
 * AbstractAuthenticationFailureEvent}s published by the {@code AuthenticationManager} (see {@code
 * SecurityConfig#authenticationEventPublisher}) into {@code AuditApplicationEvent}s, which the
 * audit module's {@code AuditEventRepository} implementation persists.
 */
@Configuration
class AuditConfig {

  @Bean
  AuthenticationAuditListener authenticationAuditListener() {
    return new AuthenticationAuditListener();
  }
}
