package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.RegistrationCredentials;
import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import eu.bbmri_eric.quality.agent.server.event.ServerRegistrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener for server registration event. Handles the communication with central servers when a
 * registration is initiated.
 */
@Component
class ServerRegistrationEventListener {

  private static final Logger log = LoggerFactory.getLogger(ServerRegistrationEventListener.class);

  private final CentralServerClientFactory clientFactory;
  private final ServerRepository serverRepository;

  ServerRegistrationEventListener(
      CentralServerClientFactory clientFactory, ServerRepository serverRepository) {
    this.clientFactory = clientFactory;
    this.serverRepository = serverRepository;
  }

  @EventListener
  @Async
  @Transactional
  void handleServerRegistration(ServerRegistrationEvent event) {
    try {
      CentralServerClient client =
          clientFactory.createClient(event.getAgentId(), event.getServerUrl(), "", "");
      RegistrationCredentials credentials = client.register();
      updateServerWithCredentials(event.getServerUrl(), credentials);
    } catch (Exception e) {
      log.error("Error registering with server {}", event.getServerUrl(), e);
      recordRegistrationFailure(event.getServerUrl(), extractErrorMessage(e));
    }
  }

  private void updateServerWithCredentials(String serverUrl, RegistrationCredentials credentials) {
    serverRepository
        .findByUrl(serverUrl)
        .ifPresentOrElse(
            server -> {
              server.addInteraction(
                  new ServerInteraction(InteractionType.REGISTRATION, "Registration request sent"));
              server.setClientId(credentials.getClientId());
              server.setClientSecret(credentials.getClientSecret());
              serverRepository.save(server);
            },
            () -> recordRegistrationFailure(serverUrl, "Server not found during registration"));
  }

  private void recordRegistrationFailure(String serverUrl, String errorMessage) {
    serverRepository
        .findByUrl(serverUrl)
        .ifPresent(
            server -> {
              server.setStatus(ServerConnectionStatus.ERROR);
              server.addInteraction(
                  new ServerInteraction(InteractionType.REGISTRATION, errorMessage));
              serverRepository.save(server);
            });
  }

  private String extractErrorMessage(Exception e) {
    String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    return message.length() > 500 ? message.substring(0, 500) : message;
  }
}
