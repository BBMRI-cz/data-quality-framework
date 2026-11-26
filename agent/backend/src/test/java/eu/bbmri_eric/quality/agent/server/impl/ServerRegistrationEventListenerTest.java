package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

class ServerRegistrationEventListenerTest {

  private static final String SERVER_URL = "https://central.example.com";
  private static final String RETRY_SUFFIX = "\nAttempts: 3 of 3. Automatic retry is scheduled.";

  private CentralServerClient client;
  private ServerRepository serverRepository;
  private Server server;
  private ServerRegistrationEventListener listener;
  private Logger listenerLogger;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    client = mock(CentralServerClient.class);
    CentralServerClientFactory clientFactory = mock(CentralServerClientFactory.class);
    when(clientFactory.createClient(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(client);
    serverRepository = mock(ServerRepository.class);
    server = new Server(SERVER_URL, "Central Server");
    when(serverRepository.findWithInteractionsByUrl(SERVER_URL)).thenReturn(Optional.of(server));
    listener = new ServerRegistrationEventListener(clientFactory, serverRepository);
    listenerLogger = (Logger) LoggerFactory.getLogger(ServerRegistrationEventListener.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    listenerLogger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    listenerLogger.detachAndStopAllAppenders();
  }

  private List<String> registrationDescriptions() {
    return server.getInteractions().stream()
        .filter(interaction -> interaction.getType() == InteractionType.REGISTRATION)
        .map(ServerInteraction::getDescription)
        .toList();
  }

  private static Stream<Arguments> retryableFailures() {
    return Stream.of(
        Arguments.of(RegistrationFailureReason.UNREACHABLE),
        Arguments.of(RegistrationFailureReason.TIMEOUT),
        Arguments.of(RegistrationFailureReason.TLS),
        Arguments.of(RegistrationFailureReason.SERVER_ERROR),
        Arguments.of(RegistrationFailureReason.RATE_LIMITED));
  }

  private static Stream<Arguments> nonRetryableFailures() {
    return Stream.of(
        Arguments.of(RegistrationFailureReason.UNAUTHORIZED),
        Arguments.of(RegistrationFailureReason.CONFLICT),
        Arguments.of(RegistrationFailureReason.CLIENT_ERROR),
        Arguments.of(RegistrationFailureReason.INCOMPLETE_RESPONSE),
        Arguments.of(RegistrationFailureReason.UNKNOWN));
  }

  @Test
  void handleServerRegistration_success_recordsAwaitingApprovalAndCredentials() {
    when(client.register()).thenReturn(new RegistrationCredentials("client-1", "secret-1"));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(serverRepository).save(server);
    assertThat(server.getClientId()).isEqualTo("client-1");
    assertThat(server.getClientSecret()).isEqualTo("secret-1");
    assertThat(server.getStatus()).isNotEqualTo(ServerConnectionStatus.ERROR);
    assertThat(registrationDescriptions())
        .containsExactly("Registration accepted by central server; awaiting approval");
  }

  @Test
  void handleServerRegistration_conflictFailure_recordsReadableFourLineDescription() {
    // message equals the ProblemDetail detail, exactly as CentralServerClientImpl produces it;
    // the status must appear only once, in the bracketed technical detail
    when(client.register())
        .thenThrow(
            RegistrationFailedException.forHttpStatus(
                HttpStatus.CONFLICT, "Agent agent-1 already exists", null));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(client).register();
    verify(serverRepository).save(server);
    assertThat(server.getStatus()).isEqualTo(ServerConnectionStatus.ERROR);
    List<String> descriptions = registrationDescriptions();
    assertThat(descriptions).hasSize(1);
    assertThat(descriptions.getFirst())
        .isEqualTo(
            """
            Registration failed: This agent is already registered on the central server
            What to check: Ask the central server administrator to remove the existing \
            registration of this agent, then remove and add the server here again.
            Technical detail: Agent agent-1 already exists [CONFLICT, HTTP 409]
            Server: https://central.example.com""");
  }

  @Test
  void handleServerRegistration_unreachableFailure_omitsHttpStatusPart() {
    when(client.register())
        .thenThrow(
            RegistrationFailedException.forReason(
                RegistrationFailureReason.UNREACHABLE, "Connection refused", null));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(client, times(3)).register();
    verify(serverRepository).save(server);
    assertThat(server.getStatus()).isEqualTo(ServerConnectionStatus.ERROR);
    List<String> descriptions = registrationDescriptions();
    assertThat(descriptions).hasSize(1);
    assertThat(descriptions.getFirst())
        .isEqualTo(
            "Registration failed: Cannot reach the central server\n"
                + "What to check: Check the server URL and port, that the host name resolves"
                + " (DNS), and that firewalls, proxies or Kubernetes NetworkPolicies allow"
                + " outgoing connections from the agent.\n"
                + "Technical detail: Connection refused [UNREACHABLE]\n"
                + "Server: "
                + SERVER_URL
                + RETRY_SUFFIX);
  }

  @Test
  void handleServerRegistration_unclassifiedFailure_recordsUnknownReason() {
    when(client.register()).thenThrow(new RuntimeException("boom"));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(client).register();
    verify(serverRepository).save(server);
    assertThat(server.getStatus()).isEqualTo(ServerConnectionStatus.ERROR);
    List<String> descriptions = registrationDescriptions();
    assertThat(descriptions).hasSize(1);
    assertThat(descriptions.getFirst())
        .isEqualTo(
            """
            Registration failed: Registration failed for an unexpected reason
            What to check: See the technical detail below and the agent log for the full stack \
            trace.
            Technical detail: boom [UNKNOWN]
            Server: https://central.example.com""");
  }

  @Test
  void handleServerRegistration_unknownReasonFailure_logsWarnWithStacktrace() {
    when(client.register())
        .thenThrow(
            RegistrationFailedException.forReason(
                RegistrationFailureReason.UNKNOWN,
                "Unexpected client failure",
                new IllegalStateException("root cause")));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    assertThat(logAppender.list).hasSize(1);
    assertThat(logAppender.list.getFirst().getThrowableProxy()).isNotNull();
  }

  @Test
  void handleServerRegistration_classifiedFailure_logsWarnWithoutStacktrace() {
    when(client.register())
        .thenThrow(
            RegistrationFailedException.forHttpStatus(
                HttpStatus.CONFLICT, "Agent agent-1 already exists", null));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    assertThat(logAppender.list).hasSize(1);
    assertThat(logAppender.list.getFirst().getThrowableProxy()).isNull();
  }

  @ParameterizedTest
  @MethodSource("retryableFailures")
  void handleServerRegistration_retryableFailure_retriesThreeTimesAndSchedulesSweep(
      RegistrationFailureReason reason) {
    when(client.register())
        .thenThrow(RegistrationFailedException.forReason(reason, "transient failure", null));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(client, times(3)).register();
    verify(serverRepository).save(server);
    assertThat(server.getStatus()).isEqualTo(ServerConnectionStatus.ERROR);
    List<String> descriptions = registrationDescriptions();
    assertThat(descriptions).hasSize(1);
    assertThat(descriptions.getFirst()).startsWith("Registration failed: ").endsWith(RETRY_SUFFIX);
    assertThat(server.getNextRegistrationRetryAt()).isNotNull();
  }

  @Test
  void handleServerRegistration_transientFailureWithExhaustedSweepRounds_requiresManualRetry() {
    when(client.register())
        .thenThrow(
            RegistrationFailedException.forReason(
                RegistrationFailureReason.TIMEOUT, "Read timed out", null));
    server.setRegistrationRetryCount(8);

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(client, times(3)).register();
    assertThat(server.getRegistrationRetryCount()).isEqualTo(8);
    assertThat(server.getNextRegistrationRetryAt()).isNull();
    assertThat(registrationDescriptions().getFirst())
        .endsWith(
            "\nAttempts: 3 of 3. Automatic retries are exhausted; retry registration manually"
                + " or remove and re-add the server.");
  }

  @ParameterizedTest
  @MethodSource("nonRetryableFailures")
  void handleServerRegistration_nonRetryableFailure_stopsAfterOneAttemptWithoutSuffix(
      RegistrationFailureReason reason) {
    when(client.register())
        .thenThrow(RegistrationFailedException.forReason(reason, "permanent failure", null));
    server.setNextRegistrationRetryAt(LocalDateTime.now().plusMinutes(5));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(client).register();
    assertThat(server.getStatus()).isEqualTo(ServerConnectionStatus.ERROR);
    assertThat(registrationDescriptions().getFirst()).doesNotContain("Attempts:");
    assertThat(server.getNextRegistrationRetryAt()).isNull();
  }

  @Test
  void handleServerRegistration_successAfterTransientFailures_clearsRetryState() {
    when(client.register())
        .thenThrow(
            RegistrationFailedException.forReason(
                RegistrationFailureReason.UNREACHABLE, "Connection refused", null))
        .thenThrow(
            RegistrationFailedException.forReason(
                RegistrationFailureReason.UNREACHABLE, "Connection refused", null))
        .thenReturn(new RegistrationCredentials("client-1", "secret-1"));
    server.setRegistrationRetryCount(3);
    server.setNextRegistrationRetryAt(LocalDateTime.now().plusMinutes(5));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(client, times(3)).register();
    assertThat(server.getClientId()).isEqualTo("client-1");
    assertThat(server.getClientSecret()).isEqualTo("secret-1");
    assertThat(server.getRegistrationRetryCount()).isNull();
    assertThat(server.getNextRegistrationRetryAt()).isNull();
    assertThat(registrationDescriptions())
        .containsExactly("Registration accepted by central server; awaiting approval");
  }

  @Test
  void handleServerRegistration_successAfterError_restoresPendingAndClearsRetryState() {
    when(client.register()).thenReturn(new RegistrationCredentials("client-1", "secret-1"));
    server.setStatus(ServerConnectionStatus.ERROR);
    server.setRegistrationRetryCount(4);
    server.setNextRegistrationRetryAt(LocalDateTime.now().plusMinutes(5));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    assertThat(server.getStatus()).isEqualTo(ServerConnectionStatus.PENDING);
    assertThat(server.getRegistrationRetryCount()).isNull();
    assertThat(server.getNextRegistrationRetryAt()).isNull();
    assertThat(registrationDescriptions())
        .containsExactly("Registration accepted by central server; awaiting approval");
  }

  @Test
  void handleServerRegistration_firstRetryLogsAttemptWaitAndReason() {
    when(client.register())
        .thenThrow(
            RegistrationFailedException.forReason(
                RegistrationFailureReason.TIMEOUT, "read timeout after 10s", null));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    assertThat(logAppender.list).hasSize(3);
    assertThat(logAppender.list.getFirst().getFormattedMessage())
        .isEqualTo(
            "Registration attempt 1/3 with server "
                + SERVER_URL
                + " failed (TIMEOUT: read timeout after 10s), retrying in 2s");
  }

  @Test
  void handleServerRegistration_serverDisappearedDuringRegistration_savesNothing() {
    when(client.register()).thenReturn(new RegistrationCredentials("client-1", "secret-1"));
    when(serverRepository.findWithInteractionsByUrl(SERVER_URL)).thenReturn(Optional.empty());

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(serverRepository, never()).save(any());
  }

  @Test
  void handleServerRegistration_success_clearsPreviousFailureAndRecordsPendingApproval() {
    when(client.register()).thenReturn(new RegistrationCredentials("client-1", "secret-1"));
    server.setRegistrationStatus(RegistrationStatus.FAILED);
    server.setRegistrationErrorReason("UNREACHABLE");
    server.setRegistrationErrorMessage("Registration failed: Cannot reach the central server");

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    assertThat(server.getRegistrationStatus())
        .isEqualTo(RegistrationStatus.REGISTERED_PENDING_APPROVAL);
    assertThat(server.getRegistrationRegisteredAt()).isNotNull();
    assertThat(server.getRegistrationErrorReason()).isNull();
    assertThat(server.getRegistrationErrorMessage()).isNull();
  }

  @Test
  void handleServerRegistration_finalFailure_recordsFailedStateMatchingInteraction() {
    when(client.register())
        .thenThrow(
            RegistrationFailedException.forHttpStatus(
                HttpStatus.UNAUTHORIZED, "Bad credentials", null));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    assertThat(server.getRegistrationStatus()).isEqualTo(RegistrationStatus.FAILED);
    assertThat(server.getRegistrationErrorReason()).isEqualTo("UNAUTHORIZED");
    assertThat(server.getRegistrationErrorMessage())
        .isEqualTo(registrationDescriptions().getFirst());
  }
}
