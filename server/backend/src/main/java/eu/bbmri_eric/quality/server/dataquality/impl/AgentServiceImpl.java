package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityAlreadyExistsException;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.AgentService;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.AgentInteractionType;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentInteractionDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistration;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentUpdateRequest;
import eu.bbmri_eric.quality.server.user.AuthenticationContextService;
import eu.bbmri_eric.quality.server.user.UserCreateDTO;
import eu.bbmri_eric.quality.server.user.UserDTO;
import eu.bbmri_eric.quality.server.user.UserRole;
import eu.bbmri_eric.quality.server.user.UserService;
import java.util.List;
import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AgentServiceImpl implements AgentService {

  private final AgentRepository agentRepository;
  private final ModelMapper modelMapper;
  private final UserService userService;
  private final AuthenticationContextService authenticationContextService;

  public AgentServiceImpl(
      AgentRepository agentRepository,
      ModelMapper modelMapper,
      UserService userService,
      AuthenticationContextService authenticationContextService) {
    this.agentRepository = agentRepository;
    this.modelMapper = modelMapper;
    this.userService = userService;
    this.authenticationContextService = authenticationContextService;
  }

  @Override
  public AgentRegistration create(AgentRegistrationRequest createAgentDto) {
    if (agentRepository.existsById(createAgentDto.getId())) {
      throw new EntityAlreadyExistsException(
          "Agent %s already exists".formatted(createAgentDto.getId()));
    }
    Agent agent = new Agent(createAgentDto.getId());
    agent.setVersion(createAgentDto.getVersion());
    Agent savedAgent = agentRepository.save(agent);
    UserDTO agentUser =
        userService.createUser(
            new UserCreateDTO("Agent %s".formatted(savedAgent.getId()), savedAgent.getId()));
    return new AgentRegistration(modelMapper.map(savedAgent, AgentDTO.class), agentUser);
  }

  @Override
  public AgentDTO update(AgentUpdateRequest updateAgentDto, String agentId) {
    UserDTO currentUser = authenticationContextService.getCurrentUser();
    if (!isAuthorizedToUpdate(currentUser, agentId)) {
      throw new AccessDeniedException("User is not authorized to update agent: " + agentId);
    }
    Agent agent = agentRepository.findById(agentId).orElseThrow(EntityNotFoundException::new);
    if (!Objects.isNull(updateAgentDto.getName())) {
      agent.setName(updateAgentDto.getName());
    }
    if (!Objects.isNull(updateAgentDto.getStatus())) {
      agent.setStatus(updateAgentDto.getStatus());
    }
    if (!Objects.isNull(updateAgentDto.getVersion())
        && !updateAgentDto.getVersion().equals(agent.getVersion())) {
      agent.setVersion(updateAgentDto.getVersion());
      agent.addInteraction(AgentInteractionType.VERSION_UPDATE);
    }
    return modelMapper.map(agent, AgentDTO.class);
  }

  @Override
  @Transactional
  public AgentDTO findById(String id) {
    return findById(id, false);
  }

  @Override
  @Transactional
  public AgentDTO findById(String id, boolean expandInteractions) {
    Agent agent =
        agentRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Agent with ID %s not found".formatted(id)));
    if (Objects.equals(authenticationContextService.getCurrentUser().getAgentId(), id)) {
      agent.addInteraction(AgentInteractionType.PING);
    }
    AgentDTO agentDTO = modelMapper.map(agent, AgentDTO.class);
    if (expandInteractions) {
      List<AgentInteractionDTO> interactions =
          agent.getInteractions().stream()
              .map(interaction -> modelMapper.map(interaction, AgentInteractionDTO.class))
              .toList();
      agentDTO.setInteractions(interactions);
    }
    return agentDTO;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AgentDTO> listAll() {
    return agentRepository.findAll().stream()
        .map(agent -> modelMapper.map(agent, AgentDTO.class))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long countAll() {
    return agentRepository.count();
  }

  @Override
  public void delete(String id) {
    Agent agent =
        agentRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Agent with ID %s not found".formatted(id)));
    agentRepository.delete(agent);
  }

  private boolean isAuthorizedToUpdate(UserDTO user, String agentId) {
    boolean isAdmin = user.getRoles() != null && user.getRoles().contains(UserRole.ADMIN);
    boolean isLinkedToAgent = agentId.equals(user.getAgentId());
    return isAdmin || isLinkedToAgent;
  }
}
