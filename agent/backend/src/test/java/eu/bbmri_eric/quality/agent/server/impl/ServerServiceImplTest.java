package eu.bbmri_eric.quality.agent.server.impl;

import static eu.bbmri_eric.quality.agent.server.impl.RegistrationRetryConstants.RETRY_DELAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.common.exception.EntityAlreadyExistsException;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.event.ServerRegistrationEvent;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.modelmapper.ModelMapper;

class ServerServiceImplTest {

  private static final String SERVER_URL = "https://central.example.com";

  private ServerRepository serverRepository;
  private EventPublisher eventPublisher;
  private ServerServiceImpl service;

  @BeforeEach
  void setUp() {
    serverRepository = mock(ServerRepository.class);
    eventPublisher = mock(EventPublisher.class);
    SettingsService settingsService = mock(SettingsService.class);
    when(settingsService.getSettings())
        .thenReturn(SettingsDTO.builder().agentId("agent-1").build());
    service =
        new ServerServiceImpl(serverRepository, new ModelMapper(), settingsService, eventPublisher);
  }

  private Server serverWithoutCredentials() {
    return new Server(SERVER_URL, "Central Server");
  }

  @Test
  void retryRegistration_unknownId_throwsEntityNotFound() {
    when(serverRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.retryRegistration("missing"))
        .isInstanceOf(EntityNotFoundException.class);

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void retryRegistration_serverWithCredentials_throwsEntityAlreadyExists() {
    Server server = serverWithoutCredentials();
    server.setClientId("client-1");
    server.setClientSecret("secret-1");
    when(serverRepository.findById(server.getId())).thenReturn(Optional.of(server));

    assertThatThrownBy(() -> service.retryRegistration(server.getId()))
        .isInstanceOf(EntityAlreadyExistsException.class);

    verify(serverRepository, never()).save(any());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void retryRegistration_serverWithoutCredentials_resetsCountAndPersistsExpiringClaim() {
    Server server = serverWithoutCredentials();
    server.setRegistrationRetryCount(5);
    server.setNextRegistrationRetryAt(LocalDateTime.now().minusMinutes(1));
    when(serverRepository.findById(server.getId())).thenReturn(Optional.of(server));
    LocalDateTime beforeDispatch = LocalDateTime.now();

    service.retryRegistration(server.getId());

    assertThat(server.getRegistrationRetryCount()).isZero();
    assertThat(server.getNextRegistrationRetryAt())
        .isBetween(
            beforeDispatch.plus(RETRY_DELAY).minusMinutes(1),
            beforeDispatch.plus(RETRY_DELAY).plusMinutes(1));
    InOrder inOrder = inOrder(serverRepository, eventPublisher);
    inOrder.verify(serverRepository).save(server);
    ArgumentCaptor<ServerRegistrationEvent> event =
        ArgumentCaptor.forClass(ServerRegistrationEvent.class);
    inOrder.verify(eventPublisher).publishEvent(event.capture());
    assertThat(event.getValue().getAgentId()).isEqualTo("agent-1");
    assertThat(event.getValue().getServerUrl()).isEqualTo(SERVER_URL);
  }
}
