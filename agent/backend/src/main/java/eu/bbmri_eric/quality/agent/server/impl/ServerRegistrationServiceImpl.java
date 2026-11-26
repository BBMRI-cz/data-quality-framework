package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.ServerRegistrationService;
import eu.bbmri_eric.quality.agent.server.domain.RegistrationStatus;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.dto.RegistrationStateDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives the API view of the registration lifecycle from the persisted registration fields of a
 * server. This is the single place mapping persisted state to {@link RegistrationStatus}: {@link
 * RegistrationStatus#ACTIVE} is never persisted and is derived here from an accepted registration
 * combined with an {@link ServerConnectionStatus#ACTIVE} server status.
 */
@Service
@Transactional(readOnly = true)
class ServerRegistrationServiceImpl implements ServerRegistrationService {

  private final ServerRepository serverRepository;

  ServerRegistrationServiceImpl(ServerRepository serverRepository) {
    this.serverRepository = serverRepository;
  }

  @Override
  public RegistrationStateDto findByServerId(String serverId) {
    Server server =
        serverRepository
            .findById(serverId)
            .orElseThrow(
                () -> new EntityNotFoundException("Server not found with id: " + serverId));
    return toDto(server);
  }

  /**
   * Maps the persisted registration fields of a server to their API view.
   *
   * @param server the server to read the registration state from
   * @return the derived registration state
   */
  static RegistrationStateDto toDto(Server server) {
    RegistrationStateDto dto = new RegistrationStateDto();
    dto.setStatus(deriveStatus(server));
    dto.setLastErrorReason(server.getRegistrationErrorReason());
    dto.setLastErrorMessage(server.getRegistrationErrorMessage());
    dto.setRegisteredAt(server.getRegistrationRegisteredAt());
    dto.setNextRetryAt(server.getNextRegistrationRetryAt());
    dto.setAttempts(
        server.getRegistrationRetryCount() == null ? 0 : server.getRegistrationRetryCount());
    return dto;
  }

  private static RegistrationStatus deriveStatus(Server server) {
    if (server.getRegistrationStatus() == RegistrationStatus.REGISTERED_PENDING_APPROVAL
        && server.getStatus() == ServerConnectionStatus.ACTIVE) {
      return RegistrationStatus.ACTIVE;
    }
    return server.getRegistrationStatus();
  }
}
