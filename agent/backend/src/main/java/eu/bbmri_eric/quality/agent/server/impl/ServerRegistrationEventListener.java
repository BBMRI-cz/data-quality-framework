package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.RegistrationCredentials;
import eu.bbmri_eric.quality.agent.server.RegistrationFailedException;
import eu.bbmri_eric.quality.agent.server.RegistrationFailureReason;
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
    String serverUrl = event.getServerUrl();
    try {
      CentralServerClient client =
          clientFactory.createClient(event.getAgentId(), serverUrl, "", "");
      RegistrationCredentials credentials = client.register();
      log.info("Registration with server {} succeeded; awaiting approval", serverUrl);
      updateServerWithCredentials(serverUrl, credentials);
    } catch (RegistrationFailedException e) {
      String failure = describeFailure(serverUrl, e);
      logFailure(serverUrl, failure, e);
      recordRegistrationFailure(serverUrl, failure);
    } catch (RuntimeException e) {
      log.error("Unexpected error while registering with server {}", serverUrl, e);
      RegistrationFailedException unexpected =
          RegistrationFailedException.forReason(
              RegistrationFailureReason.UNKNOWN, messageOrClassName(e), e);
      recordRegistrationFailure(serverUrl, describeFailure(serverUrl, unexpected));
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
   * Builds the multi-line interaction description for a failed registration attempt: a readable
   * summary and a "what to check" hint for the failure reason, the technical detail with the
   * failure message, reason and HTTP status (when known), and the server URL.
   *
   * @param serverUrl the URL of the central server
   * @param failure the classified failure
   * @return the interaction description
   */
  private String describeFailure(String serverUrl, RegistrationFailedException failure) {
    RegistrationFailureReason reason = failure.getReason();
    String statusPart =
        failure.getHttpStatus().stream()
            .mapToObj(status -> ", HTTP " + status)
            .findFirst()
            .orElse("");
    return """
        Registration failed: %s
        What to check: %s
        Technical detail: %s [%s%s]
        Server: %s"""
        .formatted(
            reason.summary(), reason.hint(), failure.getMessage(), reason, statusPart, serverUrl);
  }

  /**
   * Logs a failed registration. Unclassified failures include the stack trace, because the {@link
   * RegistrationFailureReason#UNKNOWN} hint points operators to the agent log for it.
   */
  private static void logFailure(
      String serverUrl, String description, RegistrationFailedException failure) {
    if (failure.getReason() == RegistrationFailureReason.UNKNOWN) {
      log.warn("Registration with server {} failed: {}", serverUrl, description, failure);
      return;
    }
    log.warn("Registration with server {} failed: {}", serverUrl, description);
  }

  private static String messageOrClassName(RuntimeException e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
