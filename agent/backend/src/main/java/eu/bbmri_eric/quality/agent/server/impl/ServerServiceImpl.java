package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.server.ServerService;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.dto.DetailedServerDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerCreateDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerUpdateDto;
import eu.bbmri_eric.quality.agent.server.event.ServerRegistrationEvent;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.StreamSupport;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing server operations. Decoupled from server communication concerns through
 * event publishing.
 */
@Service
@Transactional
class ServerServiceImpl implements ServerService {

  private final ServerRepository serverRepository;
  private final ModelMapper modelMapper;
  private final SettingsService settingsService;
  private final EventPublisher eventPublisher;

  public ServerServiceImpl(
      ServerRepository serverRepository,
      ModelMapper modelMapper,
      SettingsService settingsService,
      EventPublisher eventPublisher) {
    this.serverRepository = serverRepository;
    this.modelMapper = modelMapper;
    this.settingsService = settingsService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ServerDto> findAll() {
    return StreamSupport.stream(serverRepository.findAll().spliterator(), false)
        .map(server -> modelMapper.map(server, ServerDto.class))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public DetailedServerDto findById(String id) {
    Server server =
        serverRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Server not found with id: " + id));
    return modelMapper.map(server, DetailedServerDto.class);
  }

  @Override
  public ServerDto create(ServerCreateDto createDto) {
    Server server = new Server(createDto.getUrl(), createDto.getName());
    Server savedServer = serverRepository.save(server);
    String agentId = settingsService.getSettings().getAgentId();
    eventPublisher.publishEvent(new ServerRegistrationEvent(agentId, savedServer.getUrl()));
    return modelMapper.map(savedServer, ServerDto.class);
  }

  @Override
  public ServerDto update(String id, ServerUpdateDto updateDto) {
    Server server =
        serverRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Server not found with id: " + id));

    if (updateDto.getUrl() != null) {
      server.setUrl(updateDto.getUrl());
    }
    if (updateDto.getName() != null) {
      server.setName(updateDto.getName());
    }
    if (updateDto.getClientId() != null) {
      server.setClientId(updateDto.getClientId());
    }
    if (updateDto.getClientSecret() != null) {
      server.setClientSecret(updateDto.getClientSecret());
    }

    Server updatedServer = serverRepository.save(server);
    return modelMapper.map(updatedServer, ServerDto.class);
  }

  @Override
  public void deleteById(String id) {
    if (!serverRepository.existsById(id)) {
      throw new EntityNotFoundException("Server not found with id: " + id);
    }
    serverRepository.deleteById(id);
  }
}
