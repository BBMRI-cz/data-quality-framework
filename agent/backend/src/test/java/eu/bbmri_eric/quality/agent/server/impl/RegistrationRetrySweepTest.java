package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.event.ServerRegistrationEvent;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrationRetrySweepTest {

  private static final String SERVER_URL = "https://central.example.com";

  private ServerRepository serverRepository;
  private EventPublisher eventPublisher;
  private RegistrationRetrySweep sweep;

  @BeforeEach
  void setUp() {
    serverRepository = mock(ServerRepository.class);
    eventPublisher = mock(EventPublisher.class);
    SettingsService settingsService = mock(SettingsService.class);
    when(settingsService.getSettings())
        .thenReturn(SettingsDTO.builder().agentId("agent-1").build());
    sweep = new RegistrationRetrySweep(serverRepository, settingsService, eventPublisher);
  }

  private Server dueServer() {
    Server server = new Server(SERVER_URL, "Central Server");
    server.setNextRegistrationRetryAt(LocalDateTime.now().minusMinutes(1));
    return server;
  }

  @Test
  void retryDueRegistrations_dueServerWithoutCredentials_claimsRoundAndPublishesEvent() {
    Server server = dueServer();
    when(serverRepository.findAllByNextRegistrationRetryAtLessThanEqual(any()))
        .thenReturn(List.of(server));
    LocalDateTime beforeDispatch = LocalDateTime.now();

    sweep.retryDueRegistrations();

    assertThat(server.getNextRegistrationRetryAt())
        .isBetween(
            beforeDispatch.plus(RegistrationRetryConstants.RETRY_DELAY).minusMinutes(1),
            beforeDispatch.plus(RegistrationRetryConstants.RETRY_DELAY).plusMinutes(1));
    assertThat(server.getRegistrationRetryCount()).isEqualTo(1);
    verify(serverRepository).save(server);
    ArgumentCaptor<ServerRegistrationEvent> event =
        ArgumentCaptor.forClass(ServerRegistrationEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(event.getValue().getAgentId()).isEqualTo("agent-1");
    assertThat(event.getValue().getServerUrl()).isEqualTo(SERVER_URL);
  }

  @Test
  void retryDueRegistrations_serverWithUsedRounds_countsRoundUp() {
    Server server = dueServer();
    server.setRegistrationRetryCount(3);
    when(serverRepository.findAllByNextRegistrationRetryAtLessThanEqual(any()))
        .thenReturn(List.of(server));

    sweep.retryDueRegistrations();

    assertThat(server.getRegistrationRetryCount()).isEqualTo(4);
  }

  @Test
  void retryDueRegistrations_claimDueAgainAfterRestart_recoversRoundWithNewEvent() {
    Server server = dueServer();
    when(serverRepository.findAllByNextRegistrationRetryAtLessThanEqual(any()))
        .thenReturn(List.of(server));

    sweep.retryDueRegistrations();
    assertThat(server.getRegistrationRetryCount()).isEqualTo(1);

    // the claimed run never reached a persisted outcome and its claim became due across a restart
    server.setNextRegistrationRetryAt(LocalDateTime.now().minusMinutes(1));
    sweep.retryDueRegistrations();

    assertThat(server.getRegistrationRetryCount()).isEqualTo(2);
    assertThat(server.getNextRegistrationRetryAt()).isAfter(LocalDateTime.now());
    verify(eventPublisher, times(2)).publishEvent(any(ServerRegistrationEvent.class));
  }

  @Test
  void retryDueRegistrations_finalRoundClaimSurvivingRestart_republishesSameRound() {
    Server server = dueServer();
    server.setRegistrationRetryCount(RegistrationRetryConstants.MAX_SWEEP_ROUNDS);
    when(serverRepository.findAllByNextRegistrationRetryAtLessThanEqual(any()))
        .thenReturn(List.of(server));

    sweep.retryDueRegistrations();

    assertThat(server.getRegistrationRetryCount())
        .isEqualTo(RegistrationRetryConstants.MAX_SWEEP_ROUNDS);
    assertThat(server.getNextRegistrationRetryAt()).isAfter(LocalDateTime.now());
    ArgumentCaptor<ServerRegistrationEvent> event =
        ArgumentCaptor.forClass(ServerRegistrationEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(event.getValue().getServerUrl()).isEqualTo(SERVER_URL);
  }

  @Test
  void retryDueRegistrations_serverWithCredentials_skippedWithoutEvent() {
    Server server = dueServer();
    server.setClientId("client-1");
    server.setClientSecret("secret-1");
    when(serverRepository.findAllByNextRegistrationRetryAtLessThanEqual(any()))
        .thenReturn(List.of(server));

    sweep.retryDueRegistrations();

    verifyNoInteractions(eventPublisher);
    verify(serverRepository, never()).save(any());
  }

  @Test
  void retryDueRegistrations_nothingDue_touchesNothing() {
    when(serverRepository.findAllByNextRegistrationRetryAtLessThanEqual(any()))
        .thenReturn(List.of());

    sweep.retryDueRegistrations();

    verify(serverRepository, never()).save(any());
    verifyNoInteractions(eventPublisher);
  }
}
