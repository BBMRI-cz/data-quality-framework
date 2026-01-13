package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityAlreadyExistsException;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import eu.bbmri_eric.quality.server.dataquality.GroupService;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.Group;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupUpdateDTO;
import java.util.List;
import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class GroupServiceImpl implements GroupService {

  private final GroupRepository groupRepository;
  private final AgentRepository agentRepository;
  private final ModelMapper modelMapper;

  public GroupServiceImpl(
      GroupRepository groupRepository, AgentRepository agentRepository, ModelMapper modelMapper) {
    this.groupRepository = groupRepository;
    this.agentRepository = agentRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public GroupDTO create(GroupCreateDTO groupCreateDTO) {
    Objects.requireNonNull(groupCreateDTO, "GroupCreateDTO cannot be null");

    if (groupRepository.existsByName(groupCreateDTO.getName())) {
      throw new EntityAlreadyExistsException(
          "Group with name '" + groupCreateDTO.getName() + "' already exists");
    }

    Group group = new Group(groupCreateDTO.getName());
    Group savedGroup = groupRepository.save(group);
    return mapToDTO(savedGroup);
  }

  @Override
  @Transactional(readOnly = true)
  public GroupDTO findById(Long id) {
    Objects.requireNonNull(id, "ID cannot be null");
    Group group =
        groupRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Group with ID %s not found".formatted(id)));
    return mapToDTO(group);
  }

  @Override
  @Transactional(readOnly = true)
  public List<GroupDTO> findAll() {
    return groupRepository.findAll().stream().map(this::mapToDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<GroupDTO> findAll(FilterDTO filter) {
    throw new UnsupportedOperationException("Filtered pagination not yet implemented");
  }

  @Override
  public GroupDTO update(Long id, GroupUpdateDTO groupUpdateDTO) {
    Objects.requireNonNull(id, "ID cannot be null");
    Objects.requireNonNull(groupUpdateDTO, "GroupUpdateDTO cannot be null");

    Group group =
        groupRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + id));

    if (!group.getName().equals(groupUpdateDTO.getName())
        && groupRepository.existsByName(groupUpdateDTO.getName())) {
      throw new EntityAlreadyExistsException(
          "Group with name '" + groupUpdateDTO.getName() + "' already exists");
    }

    group.setName(groupUpdateDTO.getName());
    Group updatedGroup = groupRepository.save(group);
    return mapToDTO(updatedGroup);
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return groupRepository.count();
  }

  @Override
  public void delete(Long id) {
    Objects.requireNonNull(id, "ID cannot be null");
    if (!groupRepository.existsById(id)) {
      throw new EntityNotFoundException("Group not found with ID: " + id);
    }
    groupRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(Long id) {
    Objects.requireNonNull(id, "ID cannot be null");
    return groupRepository.existsById(id);
  }

  @Override
  public GroupDTO assignAgents(Long groupId, List<String> agentIds) {
    Objects.requireNonNull(groupId, "Group ID cannot be null");
    Objects.requireNonNull(agentIds, "Agent IDs cannot be null");

    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

    group.getAgents().forEach(group::removeAgent);

    for (String agentId : agentIds) {
      Agent agent =
          agentRepository
              .findById(agentId)
              .orElseThrow(
                  () -> new EntityNotFoundException("Agent not found with ID: " + agentId));
      group.addAgent(agent);
    }

    Group updatedGroup = groupRepository.save(group);
    return mapToDTO(updatedGroup);
  }

  private GroupDTO mapToDTO(Group group) {
    GroupDTO dto = modelMapper.map(group, GroupDTO.class);
    dto.setAgentIds(group.getAgents().stream().map(Agent::getId).toList());
    return dto;
  }
}
