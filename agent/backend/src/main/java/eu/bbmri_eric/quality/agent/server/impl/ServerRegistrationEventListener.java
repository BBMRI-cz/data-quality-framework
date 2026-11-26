package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.RegistrationCredentials;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
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
      log.info("Registration with server {} succeeded; awaiting approval", event.getServerUrl());
      updateServerWithCredentials(event.getServerUrl(), credentials);
    } catch (Exception e) {
      String failure = describeFailure(event.getServerUrl(), e);
      log.info("Registration attempt with server {} failed: {}", event.getServerUrl(), failure);
      recordRegistrationFailure(event.getServerUrl(), failure);
    }
  }

  private void updateServerWithCredentials(String serverUrl, RegistrationCredentials credentials) {
    serverRepository
        .findByUrl(serverUrl)
        .ifPresentOrElse(
            server -> {
              server.addInteraction(
                  new ServerInteraction(
                      InteractionType.REGISTRATION,
                      "Registration accepted by central server; awaiting approval"));
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

  /**
   * Builds the interaction description for a failed registration attempt. Classified failures carry
   * the reason and HTTP status in brackets, followed by the exception message and the server URL;
   * any other failure falls back to its plain message.
   *
   * @param serverUrl the URL of the central server
   * @param e the failure
   * @return the interaction description
   */
  private String describeFailure(String serverUrl, Exception e) {
    if (e instanceof ServerCommunicationException communication
        && communication.getReason() != null) {
      String statusPart =
          communication.getHttpStatus() != null ? ", HTTP " + communication.getHttpStatus() : "";
      String message = e.getMessage() != null ? e.getMessage() : communication.getReason().name();
      return "Registration failed [%s%s]: %s (server: %s)"
          .formatted(communication.getReason(), statusPart, message, serverUrl);
    }
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
