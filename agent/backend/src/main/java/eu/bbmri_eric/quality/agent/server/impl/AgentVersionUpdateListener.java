package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener that updates the agent version on all registered servers when the application starts.
 */
@Component
class AgentVersionUpdateListener {

  private static final Logger log = LoggerFactory.getLogger(AgentVersionUpdateListener.class);

  private final ServerRepository serverRepository;
  private final CentralServerClientFactory clientFactory;
  private final SettingsService settingsService;
  private final BuildProperties buildProperties;

  AgentVersionUpdateListener(
      ServerRepository serverRepository,
      CentralServerClientFactory clientFactory,
      SettingsService settingsService,
      BuildProperties buildProperties) {
    this.serverRepository = serverRepository;
    this.clientFactory = clientFactory;
    this.settingsService = settingsService;
    this.buildProperties = buildProperties;
  }

  @EventListener(ApplicationReadyEvent.class)
  void updateAgentVersionOnServers() {
    String version = buildProperties.getVersion();
    String agentId = settingsService.getSettings().getAgentId();

    log.info("Updating agent version {} on registered servers", version);

    serverRepository
        .findAll()
        .forEach(
            server -> {
              if (server.getClientId() != null && server.getClientSecret() != null) {
                try {
                  CentralServerClient client =
                      clientFactory.createClient(
                          agentId, server.getUrl(), server.getClientId(), server.getClientSecret());
                  client.updateAgentVersion(version);
                } catch (Exception e) {
                  log.error("Failed to update agent version on server {}", server.getUrl(), e);
                }
              } else {
                log.debug(
                    "Skipping version update for server {} - not registered yet", server.getUrl());
              }
            });
  }
}
