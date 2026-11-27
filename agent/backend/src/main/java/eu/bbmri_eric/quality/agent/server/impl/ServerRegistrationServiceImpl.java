package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.ServerRegistrationService;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.dto.RegistrationStateDto;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exposes the API view of the registration lifecycle. The actual mapping lives in the {@code
 * server.config.RegistrationStateMapper} type maps, which are the single place mapping persisted
 * state to registration status (including the derived {@code ACTIVE} state).
 */
@Service
@Transactional(readOnly = true)
class ServerRegistrationServiceImpl implements ServerRegistrationService {

  private final ServerRepository serverRepository;
  private final ModelMapper modelMapper;

  ServerRegistrationServiceImpl(ServerRepository serverRepository, ModelMapper modelMapper) {
    this.serverRepository = serverRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public RegistrationStateDto findByServerId(String serverId) {
    Server server =
        serverRepository
            .findById(serverId)
            .orElseThrow(
                () -> new EntityNotFoundException("Server not found with id: " + serverId));
    return modelMapper.map(server, RegistrationStateDto.class);
  }
}
