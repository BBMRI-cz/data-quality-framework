package eu.bbmri_eric.quality.agent.server.impl;

import static eu.bbmri_eric.quality.agent.server.impl.RegistrationRetryConstants.MAX_SWEEP_ROUNDS;
import static eu.bbmri_eric.quality.agent.server.impl.RegistrationRetryConstants.RETRY_DELAY;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.server.domain.RegistrationStatus;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.event.ServerRegistrationEvent;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for persistent registration retries. Polls every minute and re-triggers registration
 * for servers whose expiring retry claim has passed its due time (scheduled by {@link
 * ServerRegistrationEventListener} after transient failures, or left behind by a registration run
 * that never reached a persisted outcome), so a central server that becomes reachable again
 * recovers without manual action - including the case where the agent restarted while a retry was
 * pending.
 */
@Component
class RegistrationRetrySweep {

  private static final Logger log = LoggerFactory.getLogger(RegistrationRetrySweep.class);

  /** Sweep-only poll interval; the delay between failed rounds is {@code RETRY_DELAY}. */
  private static final long POLL_INTERVAL_MS = 60_000L;

  private final ServerRepository serverRepository;
  private final SettingsService settingsService;
  private final EventPublisher eventPublisher;

  RegistrationRetrySweep(
      ServerRepository serverRepository,
      SettingsService settingsService,
      EventPublisher eventPublisher) {
    this.serverRepository = serverRepository;
    this.settingsService = settingsService;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Re-triggers registration for servers whose scheduled retry is due, counting each re-trigger as
   * one sweep round towards the maximum. Deliberately not transactional: every save commits on its
   * own, so the dispatch claim - the round count-up plus a retry date {@code RETRY_DELAY} in the
   * future - is persisted before the registration event is published. Only a persisted outcome
   * clears the claim, so a run interrupted by a restart leaves a due claim that the next sweep
   * recovers; the round was already counted at dispatch, so no round is granted twice for free. A
   * due claim at the final round (only possible after such an interruption) is re-dispatched as the
   * same round, never beyond the cap.
   */
  @Scheduled(fixedDelay = POLL_INTERVAL_MS)
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
      int usedRounds =
          server.getRegistrationRetryCount() == null ? 0 : server.getRegistrationRetryCount();
      int round = Math.min(usedRounds + 1, MAX_SWEEP_ROUNDS);
      server.setRegistrationStatus(RegistrationStatus.REGISTERING);
      server.setRegistrationRetryCount(round);
      server.setNextRegistrationRetryAt(LocalDateTime.now().plus(RETRY_DELAY));
      serverRepository.save(server);
      eventPublisher.publishEvent(new ServerRegistrationEvent(agentId, server.getUrl()));
      log.info(
          "Re-triggering registration for server {} (sweep round {} of {})",
          server.getUrl(),
          round,
          MAX_SWEEP_ROUNDS);
    }
  }
}
