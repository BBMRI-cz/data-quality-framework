package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.RegistrationCredentials;
import eu.bbmri_eric.quality.agent.server.RegistrationFailureReason;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import eu.bbmri_eric.quality.agent.server.event.ServerRegistrationEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerRegistrationEventListenerTest {

  private static final String SERVER_URL = "https://central.example.com";

  private CentralServerClient client;
  private ServerRepository serverRepository;
  private Server server;
  private ServerRegistrationEventListener listener;

  @BeforeEach
  void setUp() {
    client = mock(CentralServerClient.class);
    CentralServerClientFactory clientFactory = mock(CentralServerClientFactory.class);
    when(clientFactory.createClient(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(client);
    serverRepository = mock(ServerRepository.class);
    server = new Server(SERVER_URL, "Central Server");
    when(serverRepository.findByUrl(SERVER_URL)).thenReturn(Optional.of(server));
    listener = new ServerRegistrationEventListener(clientFactory, serverRepository);
  }

  private List<String> registrationDescriptions() {
    return server.getInteractions().stream()
        .filter(interaction -> interaction.getType() == InteractionType.REGISTRATION)
        .map(ServerInteraction::getDescription)
        .toList();
  }

  @Test
  void handleServerRegistration_success_recordsAwaitingApprovalAndCredentials() throws Exception {
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
  void handleServerRegistration_classifiedFailure_recordsReasonAndStatus() throws Exception {
    // message equals the ProblemDetail detail, exactly as CentralServerClientImpl produces it;
    // the status must appear only once, in the bracketed prefix
    when(client.register())
        .thenThrow(
            new ServerCommunicationException(
                "Agent agent-1 already exists",
                RegistrationFailureReason.CONFLICT,
                409,
                "Agent agent-1 already exists",
                null));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(serverRepository).save(server);
    assertThat(server.getStatus()).isEqualTo(ServerConnectionStatus.ERROR);
    List<String> descriptions = registrationDescriptions();
    assertThat(descriptions).hasSize(1);
    assertThat(descriptions.getFirst())
        .isEqualTo(
            "Registration failed [CONFLICT, HTTP 409]: Agent agent-1 already exists (server: "
                + SERVER_URL
                + ")");
  }

  @Test
  void handleServerRegistration_unclassifiedFailure_recordsGenericMessage() throws Exception {
    when(client.register()).thenThrow(new RuntimeException("boom"));

    listener.handleServerRegistration(new ServerRegistrationEvent("agent-1", SERVER_URL));

    verify(serverRepository).save(server);
    assertThat(server.getStatus()).isEqualTo(ServerConnectionStatus.ERROR);
    assertThat(registrationDescriptions()).containsExactly("boom");
  }
}
