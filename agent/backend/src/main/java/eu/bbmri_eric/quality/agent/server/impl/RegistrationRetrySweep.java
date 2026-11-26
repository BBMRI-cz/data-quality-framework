package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.event.ServerRegistrationEvent;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for persistent registration retries. Re-triggers registration for servers whose
 * automatic retry date has passed (scheduled by {@link ServerRegistrationEventListener} after
 * transient failures), so a central server that becomes reachable again recovers without manual
 * action - including the case where the agent started before the central server was up.
 */
@Component
class RegistrationRetrySweep {

  private static final Logger log = LoggerFactory.getLogger(RegistrationRetrySweep.class);

  private final ServerRepository serverRepository;
  private final SettingsService settingsService;
  private final EventPublisher eventPublisher;
  private final int maxSweepRounds;

  RegistrationRetrySweep(
      ServerRepository serverRepository,
      SettingsService settingsService,
      EventPublisher eventPublisher,
      @Value("${agent.central-server.registration.max-sweep-rounds:8}") int maxSweepRounds) {
    this.serverRepository = serverRepository;
    this.settingsService = settingsService;
    this.eventPublisher = eventPublisher;
    this.maxSweepRounds = maxSweepRounds;
  }

  /**
   * Re-triggers registration for servers whose scheduled retry is due, counting each re-trigger as
   * one sweep round towards the configured maximum. Deliberately not transactional: every save
   * commits on its own, so the claim (clearing the retry date and counting the round up) is
   * persisted before the registration event is published.
   */
  @Scheduled(fixedDelayString = "${agent.central-server.registration.sweep-interval:15m}")
  public void retryDueRegistrations() {
    String agentId = settingsService.getSettings().getAgentId();
    for (Server server :
        serverRepository.findAllByNextRegistrationRetryAtLessThanEqual(LocalDateTime.now())) {
      if (!server.getClientId().isEmpty()) {
        log.info(
            "Skipping registration retry for server {}: it already holds registration credentials",
            server.getUrl());
        continue;
      }
      int round =
          (server.getRegistrationRetryCount() == null ? 0 : server.getRegistrationRetryCount()) + 1;
      server.setNextRegistrationRetryAt(null);
      server.setRegistrationRetryCount(round);
      serverRepository.save(server);
      eventPublisher.publishEvent(new ServerRegistrationEvent(agentId, server.getUrl()));
      log.info(
          "Re-triggering registration for server {} (sweep round {} of {})",
          server.getUrl(),
          round,
          maxSweepRounds);
    }
  }
}
