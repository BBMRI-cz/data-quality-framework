package eu.bbmri_eric.quality.agent.server.impl;

import static eu.bbmri_eric.quality.agent.server.impl.RegistrationRetryConstants.INITIAL_BACKOFF;
import static eu.bbmri_eric.quality.agent.server.impl.RegistrationRetryConstants.MAX_ATTEMPTS;
import static eu.bbmri_eric.quality.agent.server.impl.RegistrationRetryConstants.MAX_SWEEP_ROUNDS;
import static eu.bbmri_eric.quality.agent.server.impl.RegistrationRetryConstants.RETRY_DELAY;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.RegistrationCredentials;
import eu.bbmri_eric.quality.agent.server.RegistrationFailedException;
import eu.bbmri_eric.quality.agent.server.RegistrationFailureReason;
import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import eu.bbmri_eric.quality.agent.server.domain.RegistrationStatus;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import eu.bbmri_eric.quality.agent.server.event.ServerRegistrationEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for server registration event. Handles the communication with central servers when a
 * registration is initiated, retrying transient failures in-process with exponential backoff. When
 * the in-process attempts are exhausted and the failure was transient, the next retry is scheduled
 * persistently via {@code nextRegistrationRetryAt} and picked up by {@link RegistrationRetrySweep}.
 * A persisted success returns the server to {@link ServerConnectionStatus#PENDING} so the
 * health-check scheduler resumes approval polling.
 *
 * <p>Runs without a surrounding transaction on purpose: neither the HTTP calls nor the backoff
 * sleeps may hold a database connection. Every read-modify-write path loads the server with its
 * interactions eagerly ({@link ServerRepository#findWithInteractionsByUrl}) so the detached entity
 * can be merged safely.
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
  void handleServerRegistration(ServerRegistrationEvent event) {
    String serverUrl = event.getServerUrl();
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        CentralServerClient client =
            clientFactory.createClient(event.getAgentId(), serverUrl, "", "");
        RegistrationCredentials credentials = client.register();
        log.info("Registration with server {} succeeded; awaiting approval", serverUrl);
        updateServerWithCredentials(serverUrl, credentials);
        return;
      } catch (RegistrationFailedException e) {
        if (!e.getReason().isRetryable() || attempt == MAX_ATTEMPTS) {
          recordFinalFailure(serverUrl, e, attempt);
          return;
        }
        Duration wait = INITIAL_BACKOFF.multipliedBy(1L << (attempt - 1));
        log.warn(
            "Registration attempt {}/{} with server {} failed ({}: {}), retrying in {}",
            attempt,
            MAX_ATTEMPTS,
            serverUrl,
            e.getReason(),
            e.getMessage(),
            wait.toSeconds() + "s");
        if (!sleep(wait)) {
          recordFinalFailure(serverUrl, e, attempt);
          return;
        }
      } catch (RuntimeException e) {
        log.error("Unexpected error while registering with server {}", serverUrl, e);
        RegistrationFailedException unexpected =
            RegistrationFailedException.forReason(
                RegistrationFailureReason.UNKNOWN, messageOrClassName(e), e);
        recordFinalFailure(serverUrl, unexpected, 1);
        return;
      }
    }
  }

  private void updateServerWithCredentials(String serverUrl, RegistrationCredentials credentials) {
    serverRepository
        .findWithInteractionsByUrl(serverUrl)
        .ifPresentOrElse(
            server -> {
              server.addInteraction(
                  new ServerInteraction(
                      InteractionType.REGISTRATION,
                      "Registration accepted by central server; awaiting approval"));
              server.setClientId(credentials.getClientId());
              server.setClientSecret(credentials.getClientSecret());
              server.setStatus(ServerConnectionStatus.PENDING);
              server.setRegistrationStatus(RegistrationStatus.REGISTERED_PENDING_APPROVAL);
              server.setRegistrationRegisteredAt(LocalDateTime.now());
              server.setRegistrationErrorReason(null);
              server.setRegistrationErrorMessage(null);
              server.setRegistrationRetryCount(null);
              server.setNextRegistrationRetryAt(null);
              serverRepository.save(server);
            },
            () -> log.warn("Server {} not found during registration", serverUrl));
  }

  /**
   * Records the final failed outcome for a registration: the server goes to {@link
   * ServerConnectionStatus#ERROR} with one interaction and, for transient failures with sweep
   * rounds left, the next persistent retry is scheduled. Any pending retry date is cleared
   * otherwise, so a failure that must not be retried automatically cancels a scheduled sweep.
   */
  private void recordFinalFailure(
      String serverUrl, RegistrationFailedException failure, int attempts) {
    logFailure(serverUrl, describeFailure(serverUrl, failure), failure);
    serverRepository
        .findWithInteractionsByUrl(serverUrl)
        .ifPresent(
            server -> {
              boolean retryScheduled =
                  failure.getReason().isRetryable() && usedSweepRounds(server) < MAX_SWEEP_ROUNDS;
              String description =
                  describeAttemptedFailure(serverUrl, failure, attempts, retryScheduled);
              server.setStatus(ServerConnectionStatus.ERROR);
              server.addInteraction(
                  new ServerInteraction(InteractionType.REGISTRATION, description));
              server.setRegistrationStatus(RegistrationStatus.FAILED);
              server.setRegistrationErrorReason(failure.getReason().name());
              server.setRegistrationErrorMessage(description);
              server.setNextRegistrationRetryAt(
                  retryScheduled ? LocalDateTime.now().plus(RETRY_DELAY) : null);
              serverRepository.save(server);
            });
  }

  /**
   * Builds the interaction description for a registration that was attempted more than once: the
   * {@link #describeFailure} description plus a line with the attempt count and the retry outcome.
   * Single-attempt failures keep the plain description.
   *
   * @param serverUrl the URL of the central server
   * @param failure the classified failure
   * @param attempts the number of registration attempts that were made
   * @param retryScheduled whether a persistent automatic retry was scheduled
   * @return the interaction description
   */
  private String describeAttemptedFailure(
      String serverUrl, RegistrationFailedException failure, int attempts, boolean retryScheduled) {
    String description = describeFailure(serverUrl, failure);
    if (attempts <= 1) {
      return description;
    }
    String outcome =
        retryScheduled
            ? "Automatic retry is scheduled."
            : "Automatic retries are exhausted; retry registration manually"
                + " or remove and re-add the server.";
    return description + "\nAttempts: %d of %d. %s".formatted(attempts, MAX_ATTEMPTS, outcome);
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

  /**
   * Sleeps between registration attempts, occupying only the async task thread.
   *
   * @param wait how long to wait
   * @return false when the thread was interrupted and retrying must be aborted
   */
  private static boolean sleep(Duration wait) {
    try {
      Thread.sleep(wait.toMillis());
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static int usedSweepRounds(Server server) {
    return server.getRegistrationRetryCount() == null ? 0 : server.getRegistrationRetryCount();
  }

  private static String messageOrClassName(RuntimeException e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
