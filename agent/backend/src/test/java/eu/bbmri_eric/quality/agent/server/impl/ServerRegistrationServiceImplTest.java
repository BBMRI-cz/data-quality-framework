package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.server.config.RegistrationStateMapper;
import eu.bbmri_eric.quality.agent.server.domain.RegistrationStatus;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.dto.DetailedServerDto;
import eu.bbmri_eric.quality.agent.server.dto.RegistrationStateDto;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

class ServerRegistrationServiceImplTest {

  private static final String SERVER_URL = "https://central.example.com";

  private ServerRepository serverRepository;
  private ModelMapper modelMapper;
  private ServerRegistrationServiceImpl service;

  @BeforeEach
  void setUp() {
    serverRepository = mock(ServerRepository.class);
    modelMapper = new ModelMapper();
    new RegistrationStateMapper(modelMapper).addMappings();
    service = new ServerRegistrationServiceImpl(serverRepository, modelMapper);
  }

  private Server server() {
    return new Server(SERVER_URL, "Central Server");
  }

  @Test
  void findByServerId_unknownId_throwsEntityNotFound() {
    when(serverRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findByServerId("missing"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void findByServerId_mapsPersistedFailureFields() {
    Server server = server();
    server.setRegistrationStatus(RegistrationStatus.FAILED);
    server.setRegistrationErrorReason("UNREACHABLE");
    server.setRegistrationErrorMessage("Registration failed: Cannot reach the central server");
    server.setRegistrationRetryCount(3);
    server.setNextRegistrationRetryAt(LocalDateTime.now().plusMinutes(15));
    when(serverRepository.findById(server.getId())).thenReturn(Optional.of(server));

    RegistrationStateDto dto = service.findByServerId(server.getId());

    assertThat(dto.getStatus()).isEqualTo(RegistrationStatus.FAILED);
    assertThat(dto.getLastErrorReason()).isEqualTo("UNREACHABLE");
    assertThat(dto.getLastErrorMessage())
        .isEqualTo("Registration failed: Cannot reach the central server");
    assertThat(dto.getAttempts()).isEqualTo(3);
    assertThat(dto.getNextRetryAt()).isNotNull();
  }

  @Test
  void findByServerId_neverAttempted_mapsAttemptsToZero() {
    Server server = server();
    when(serverRepository.findById(server.getId())).thenReturn(Optional.of(server));

    RegistrationStateDto dto = service.findByServerId(server.getId());

    assertThat(dto.getStatus()).isEqualTo(RegistrationStatus.UNREGISTERED);
    assertThat(dto.getAttempts()).isZero();
    assertThat(dto.getLastErrorReason()).isNull();
    assertThat(dto.getLastErrorMessage()).isNull();
    assertThat(dto.getRegisteredAt()).isNull();
  }

  @Test
  void findByServerId_acceptedRegistrationWithActiveServer_derivesActive() {
    Server server = server();
    server.setRegistrationStatus(RegistrationStatus.REGISTERED_PENDING_APPROVAL);
    server.setStatus(ServerConnectionStatus.ACTIVE);
    server.setRegistrationRegisteredAt(LocalDateTime.now().minusDays(1));
    when(serverRepository.findById(server.getId())).thenReturn(Optional.of(server));

    RegistrationStateDto dto = service.findByServerId(server.getId());

    assertThat(dto.getStatus()).isEqualTo(RegistrationStatus.ACTIVE);
    assertThat(dto.getRegisteredAt()).isNotNull();
  }

  @Test
  void findByServerId_acceptedRegistrationWithPendingServer_staysPendingApproval() {
    Server server = server();
    server.setRegistrationStatus(RegistrationStatus.REGISTERED_PENDING_APPROVAL);
    when(serverRepository.findById(server.getId())).thenReturn(Optional.of(server));

    RegistrationStateDto dto = service.findByServerId(server.getId());

    assertThat(dto.getStatus()).isEqualTo(RegistrationStatus.REGISTERED_PENDING_APPROVAL);
  }

  @Test
  void findByServerId_errorServerStatusDoesNotOverrideFailedState() {
    Server server = server();
    server.setRegistrationStatus(RegistrationStatus.FAILED);
    server.setStatus(ServerConnectionStatus.ERROR);
    when(serverRepository.findById(server.getId())).thenReturn(Optional.of(server));

    RegistrationStateDto dto = service.findByServerId(server.getId());

    assertThat(dto.getStatus()).isEqualTo(RegistrationStatus.FAILED);
  }

  @Test
  void typeMaps_validate() {
    Server server = server();
    server.setRegistrationStatus(RegistrationStatus.FAILED);
    modelMapper.map(server, RegistrationStateDto.class);
    modelMapper.map(server, DetailedServerDto.class);

    modelMapper.validate();
  }
}
