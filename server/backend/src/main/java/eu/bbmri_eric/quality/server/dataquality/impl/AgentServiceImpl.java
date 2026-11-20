package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityAlreadyExistsException;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.common.TooManyRequestsException;
import eu.bbmri_eric.quality.server.dataquality.AgentService;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.AgentInteractionType;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentInteractionDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistration;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentUpdateRequest;
import eu.bbmri_eric.quality.server.user.AuthenticationContextService;
import eu.bbmri_eric.quality.server.user.UserRole;
import eu.bbmri_eric.quality.server.user.UserService;
import eu.bbmri_eric.quality.server.user.dto.UserCreateDTO;
import eu.bbmri_eric.quality.server.user.dto.UserDTO;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
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
  public AgentRegistration create(AgentRegistrationRequest createAgentDto, String ipAddress) {
    if (agentRepository.existsById(createAgentDto.getId())) {
      throw new EntityAlreadyExistsException(
          "Agent %s already exists".formatted(createAgentDto.getId()));
    }
    String canonicalIp = IpAddressNormalizer.normalize(ipAddress);
    if (canonicalIp != null) {
      findAgentAtIp(canonicalIp, ipAddress.trim())
          .ifPresent(
              conflict -> {
                throw alreadyRegisteredFromIp(conflict, canonicalIp);
              });
    }
    // Keep the raw value when it cannot be parsed, so the address is never silently lost.
    Agent agent = new Agent(createAgentDto.getId(), canonicalIp != null ? canonicalIp : ipAddress);
    agent.setVersion(createAgentDto.getVersion());
    Agent savedAgent;
    try {
      savedAgent = agentRepository.save(agent);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityAlreadyExistsException(
          "Agent %s already exists".formatted(createAgentDto.getId()));
    }
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
    userService.deleteUser(id);
    agentRepository.delete(agent);
  }

  /**
   * Rows stored before IP normalization may hold a different spelling of the same address, so
   * candidates are fetched by both the raw and the canonical form and re-normalized in Java.
   */
  private Optional<Agent> findAgentAtIp(String canonicalIp, String rawIp) {
    List<String> lookupKeys =
        rawIp.equals(canonicalIp) ? List.of(canonicalIp) : List.of(rawIp, canonicalIp);
    return agentRepository.findByIpAddressIn(lookupKeys).stream()
        .filter(
            candidate ->
                canonicalIp.equals(IpAddressNormalizer.normalize(candidate.getIpAddress())))
        .findFirst();
  }

  private TooManyRequestsException alreadyRegisteredFromIp(Agent conflict, String ipAddress) {
    String label =
        conflict.getName() == null
            ? conflict.getId()
            : "%s (%s)".formatted(conflict.getId(), conflict.getName());
    return new TooManyRequestsException(
        "Agent %s is already registered from IP address %s; delete that agent in the server UI"
            + " (DELETE /api/v1/agents/%s) to free the address, then register again."
                .formatted(label, ipAddress, conflict.getId()));
  }

  private boolean isAuthorizedToUpdate(UserDTO user, String agentId) {
    boolean isAdmin = user.getRoles() != null && user.getRoles().contains(UserRole.ADMIN);
    boolean isLinkedToAgent = agentId.equals(user.getAgentId());
    return isAdmin || isLinkedToAgent;
  }
}
