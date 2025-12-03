package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodic server health checks and registration status updates. Manages the
 * scheduled tasks for monitoring server connections without coupling the service layer to
 * scheduling concerns.
 */
@Component
class ServerHealthCheckScheduler {

  private static final Logger log = LoggerFactory.getLogger(ServerHealthCheckScheduler.class);

  private final ServerRepository serverRepository;
  private final SettingsService settingsService;
  private final CentralServerClientFactory clientFactory;

  ServerHealthCheckScheduler(
      ServerRepository serverRepository,
      SettingsService settingsService,
      CentralServerClientFactory clientFactory) {
    this.serverRepository = serverRepository;
    this.settingsService = settingsService;
    this.clientFactory = clientFactory;
  }

  /**
   * Periodically checks the registration status of servers with PENDING status. Updates server
   * status based on the health check results. Runs every minute to provide faster feedback during
   * registration.
   */
  @Scheduled(fixedRate = 60000)
  @Transactional
  public void checkPendingServerStatuses() {
    String agentId = settingsService.getSettings().getAgentId();
    serverRepository
        .findAllByStatusIs(ServerConnectionStatus.PENDING)
        .forEach(server -> checkServerStatus(server, agentId));
  }

  /**
   * Periodically checks the registration status of all servers. Updates server status based on the
   * health check results. Runs every hour.
   */
  @Scheduled(fixedRate = 3600000)
  @Transactional
  public void checkAllServerStatuses() {
    String agentId = settingsService.getSettings().getAgentId();
    serverRepository.findAll().forEach(server -> checkServerStatus(server, agentId));
  }

  private void checkServerStatus(Server server, String agentId) {
    try {
      var client =
          clientFactory.createClient(
              agentId, server.getUrl(), server.getClientId(), server.getClientSecret());
      var newStatus = client.checkRegistrationStatus();
      updateServerStatus(server, newStatus);
    } catch (Exception e) {
      handleStatusCheckError(server, e);
    }
    serverRepository.save(server);
  }

  private void updateServerStatus(Server server, ServerConnectionStatus newStatus) {
    if (newStatus != server.getStatus()) {
      server.setStatus(newStatus);
    }
  }

  private void handleStatusCheckError(Server server, Exception e) {
    server.setStatus(ServerConnectionStatus.ERROR);
    String errorMessage = extractErrorMessage(e);
    server.addInteraction(new ServerInteraction(InteractionType.STATUS_CHECK, errorMessage));
    log.error("Error checking status for server {}", server.getUrl(), e);
  }

  private String extractErrorMessage(Exception e) {
    String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    return message.length() > 500 ? message.substring(0, 500) : message;
  }
}
