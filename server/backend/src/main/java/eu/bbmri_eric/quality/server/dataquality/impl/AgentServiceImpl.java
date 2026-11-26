package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityAlreadyExistsException;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.common.TooManyRequestsException;
import eu.bbmri_eric.quality.server.dataquality.AgentService;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.AgentInteractionType;
import eu.bbmri_eric.quality.server.dataquality.domain.AgentStatus;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentInteractionDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistration;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationResult;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AgentServiceImpl implements AgentService {

  private static final Logger logger = LoggerFactory.getLogger(AgentServiceImpl.class);

  private static final String IPV4_MAPPED_IPV6_PREFIX = "::ffff:";

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
  public AgentRegistrationResult create(AgentRegistrationRequest createAgentDto, String ipAddress) {
    String clientIpAddress = toBareIpAddress(ipAddress);
    Optional<Agent> existingAgent = agentRepository.findByIdForUpdate(createAgentDto.getId());
    if (existingAgent.isPresent()) {
      return reRegister(existingAgent.get(), createAgentDto, clientIpAddress);
    }
    if (clientIpAddress != null) {
      Optional<Agent> conflictingAgent = agentRepository.findFirstByIpAddress(clientIpAddress);
      if (conflictingAgent.isPresent()) {
        Agent conflicting = conflictingAgent.get();
        String detail =
            conflicting.getName() == null
                ? ("Another agent is already registered from IP address %s. Delete that agent in"
                        + " the server UI to register a new agent from this address.")
                    .formatted(clientIpAddress)
                : ("Another agent (%s) is already registered from IP address %s. Delete that agent"
                        + " in the server UI to register a new agent from this address.")
                    .formatted(conflicting.getName(), clientIpAddress);
        throw new TooManyRequestsException(detail);
      }
    }
    Agent agent = new Agent(createAgentDto.getId(), clientIpAddress);
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
    return new AgentRegistrationResult(
        new AgentRegistration(modelMapper.map(savedAgent, AgentDTO.class), agentUser), false);
  }

  private AgentRegistrationResult reRegister(
      Agent agent, AgentRegistrationRequest createAgentDto, String clientIpAddress) {
    if (!Objects.equals(clientIpAddress, agent.getIpAddress())) {
      throw new EntityAlreadyExistsException(
          ("Agent %s is already registered from IP address %s. Re-registration is only allowed"
                  + " from the same address (this request came from %s). Delete this agent in the"
                  + " server UI to register again from this address.")
              .formatted(agent.getId(), agent.getIpAddress(), clientIpAddress));
    }
    if (agent.getStatus() != AgentStatus.PENDING) {
      throw new EntityAlreadyExistsException(
          ("Agent %s has already been approved (status %s) and cannot re-register. An"
                  + " administrator must delete it in the server UI before it can register again.")
              .formatted(agent.getId(), agent.getStatus()));
    }
    agent.addInteraction(AgentInteractionType.REGISTRATION);
    if (!Objects.equals(createAgentDto.getVersion(), agent.getVersion())) {
      agent.setVersion(createAgentDto.getVersion());
    }
    UserDTO agentUser = userService.rotateAgentPassword(agent.getId());
    Agent savedAgent = agentRepository.save(agent);
    logger.info(
        "Agent {} re-registered from IP {}; rotated its credentials (previous password invalidated)",
        agent.getId(),
        clientIpAddress);
    return new AgentRegistrationResult(
        new AgentRegistration(modelMapper.map(savedAgent, AgentDTO.class), agentUser), true);
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

  private boolean isAuthorizedToUpdate(UserDTO user, String agentId) {
    boolean isAdmin = user.getRoles() != null && user.getRoles().contains(UserRole.ADMIN);
    boolean isLinkedToAgent = agentId.equals(user.getAgentId());
    return isAdmin || isLinkedToAgent;
  }

  /**
   * Dual-stack containers report an IPv4 client as an IPv4-mapped IPv6 address (for example {@code
   * ::ffff:10.0.0.5}); such addresses are compared and stored as their plain IPv4 form.
   */
  private static String toBareIpAddress(String ipAddress) {
    if (ipAddress == null
        || !ipAddress.regionMatches(
            true, 0, IPV4_MAPPED_IPV6_PREFIX, 0, IPV4_MAPPED_IPV6_PREFIX.length())) {
      return ipAddress;
    }
    String mappedAddress = ipAddress.substring(IPV4_MAPPED_IPV6_PREFIX.length());
    return mappedAddress.contains(".") ? mappedAddress : ipAddress;
  }
}
