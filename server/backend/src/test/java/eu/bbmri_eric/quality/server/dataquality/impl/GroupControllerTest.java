package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.Group;
import eu.bbmri_eric.quality.server.dataquality.dto.AssignAgentsDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupUpdateDTO;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class GroupControllerTest {

  private static final String API_V1_GROUPS = "/api/v1/groups";
  private static final String API_V1_GROUPS_ID = "/api/v1/groups/{id}";
  private static final String API_V1_GROUPS_ID_AGENTS = "/api/v1/groups/{id}/agents";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private GroupRepository groupRepository;
  @Autowired private AgentRepository agentRepository;

  private Group testGroup;
  private Agent testAgent1;
  private Agent testAgent2;

  @BeforeEach
  void setUp() {
    groupRepository.deleteAll();
    agentRepository.deleteAll();

    testGroup = new Group("Production Agents");
    testGroup = groupRepository.save(testGroup);

    testAgent1 = new Agent("550e8400-e29b-41d4-a716-446655440001");
    testAgent1.setName("Agent One");
    testAgent1 = agentRepository.save(testAgent1);

    testAgent2 = new Agent("550e8400-e29b-41d4-a716-446655440002");
    testAgent2.setName("Agent Two");
    testAgent2 = agentRepository.save(testAgent2);
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnGroupWithHateoasLinksWhenExists() throws Exception {
    mockMvc
        .perform(get(API_V1_GROUPS_ID, testGroup.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testGroup.getId()))
        .andExpect(jsonPath("$.name").value("Production Agents"))
        .andExpect(jsonPath("$.agentIds").isArray())
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/groups/" + testGroup.getId()))
        .andExpect(jsonPath("$._links.groups.href").value("http://localhost/api/v1/groups"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldReturnGroupForAdmin() throws Exception {
    mockMvc
        .perform(get(API_V1_GROUPS_ID, testGroup.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testGroup.getId()));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnNotFoundWhenGroupDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;

    mockMvc.perform(get(API_V1_GROUPS_ID, nonExistentId)).andExpect(status().isNotFound());
  }

  @Test
  void findById_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc.perform(get(API_V1_GROUPS_ID, testGroup.getId())).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnEmptyListWithHateoasLinksWhenNoGroups() throws Exception {
    groupRepository.deleteAll();

    mockMvc
        .perform(get(API_V1_GROUPS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/groups"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnAllGroupsWithHateoasLinks() throws Exception {
    Group secondGroup = new Group("Development Agents");
    groupRepository.save(secondGroup);

    mockMvc
        .perform(get(API_V1_GROUPS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.groups").isArray())
        .andExpect(jsonPath("$._embedded.groups.length()").value(2))
        .andExpect(jsonPath("$._embedded.groups[?(@.name == 'Production Agents')]").exists())
        .andExpect(jsonPath("$._embedded.groups[?(@.name == 'Development Agents')]").exists())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/groups"))
        .andExpect(jsonPath("$._embedded.groups[0]._links.self.href").exists())
        .andExpect(jsonPath("$._embedded.groups[1]._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findAll_shouldReturnAllGroupsForAdmin() throws Exception {
    mockMvc
        .perform(get(API_V1_GROUPS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.groups").isArray())
        .andExpect(jsonPath("$._embedded.groups.length()").value(1));
  }

  @Test
  void findAll_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc.perform(get(API_V1_GROUPS)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldCreateGroupAndReturnCreatedStatusWithHateoasLinks() throws Exception {
    GroupCreateDTO createDTO = new GroupCreateDTO("Test Group");

    mockMvc
        .perform(
            post(API_V1_GROUPS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Test Group"))
        .andExpect(jsonPath("$.agentIds").isArray())
        .andExpect(jsonPath("$._links.self.href").exists())
        .andExpect(jsonPath("$._links.groups.href").value("http://localhost/api/v1/groups"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnConflictForDuplicateName() throws Exception {
    GroupCreateDTO createDTO = new GroupCreateDTO("Production Agents");

    mockMvc
        .perform(
            post(API_V1_GROUPS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForEmptyName() throws Exception {
    GroupCreateDTO createDTO = new GroupCreateDTO("");

    mockMvc
        .perform(
            post(API_V1_GROUPS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void create_shouldReturnForbiddenForNonAdminUser() throws Exception {
    GroupCreateDTO createDTO = new GroupCreateDTO("Test Group");

    mockMvc
        .perform(
            post(API_V1_GROUPS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void create_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    GroupCreateDTO createDTO = new GroupCreateDTO("Test Group");

    mockMvc
        .perform(
            post(API_V1_GROUPS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldUpdateGroupAndReturnHateoasResponse() throws Exception {
    GroupUpdateDTO updateDTO = new GroupUpdateDTO("Updated Group Name");

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testGroup.getId()))
        .andExpect(jsonPath("$.name").value("Updated Group Name"))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/groups/" + testGroup.getId()))
        .andExpect(jsonPath("$._links.groups.href").value("http://localhost/api/v1/groups"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnNotFoundWhenGroupDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;
    GroupUpdateDTO updateDTO = new GroupUpdateDTO("Updated Name");

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID, nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnConflictWhenNameAlreadyExists() throws Exception {
    Group anotherGroup = new Group("Another Group");
    groupRepository.save(anotherGroup);

    GroupUpdateDTO updateDTO = new GroupUpdateDTO("Another Group");

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnBadRequestForInvalidData() throws Exception {
    GroupUpdateDTO updateDTO = new GroupUpdateDTO("");

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void update_shouldReturnForbiddenForNonAdminUser() throws Exception {
    GroupUpdateDTO updateDTO = new GroupUpdateDTO("Updated Name");

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void update_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    GroupUpdateDTO updateDTO = new GroupUpdateDTO("Updated Name");

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_shouldDeleteGroupAndReturnNoContent() throws Exception {
    mockMvc.perform(delete(API_V1_GROUPS_ID, testGroup.getId())).andExpect(status().isNoContent());

    mockMvc.perform(get(API_V1_GROUPS_ID, testGroup.getId())).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_shouldReturnNotFoundWhenGroupDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;

    mockMvc.perform(delete(API_V1_GROUPS_ID, nonExistentId)).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void delete_shouldReturnForbiddenForNonAdminUser() throws Exception {
    mockMvc.perform(delete(API_V1_GROUPS_ID, testGroup.getId())).andExpect(status().isForbidden());
  }

  @Test
  void delete_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc
        .perform(delete(API_V1_GROUPS_ID, testGroup.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignAgents_shouldAssignAgentsToGroupAndReturnUpdatedGroup() throws Exception {
    List<String> agentIds = Arrays.asList(testAgent1.getId(), testAgent2.getId());
    AssignAgentsDTO assignDTO = new AssignAgentsDTO(agentIds);

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID_AGENTS, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testGroup.getId()))
        .andExpect(jsonPath("$.name").value("Production Agents"))
        .andExpect(jsonPath("$.agentIds").isArray())
        .andExpect(jsonPath("$.agentIds.length()").value(2))
        .andExpect(jsonPath("$.agentIds[0]").value(testAgent1.getId()))
        .andExpect(jsonPath("$.agentIds[1]").value(testAgent2.getId()))
        .andExpect(jsonPath("$._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignAgents_shouldReplaceExistingAgents() throws Exception {
    testGroup.addAgent(testAgent1);
    groupRepository.save(testGroup);

    List<String> agentIds = List.of(testAgent2.getId());
    AssignAgentsDTO assignDTO = new AssignAgentsDTO(agentIds);

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID_AGENTS, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.agentIds.length()").value(1))
        .andExpect(jsonPath("$.agentIds[0]").value(testAgent2.getId()));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignAgents_shouldClearAgentsWhenEmptyListProvided() throws Exception {
    testGroup.addAgent(testAgent1);
    testGroup.addAgent(testAgent2);
    groupRepository.save(testGroup);

    AssignAgentsDTO assignDTO = new AssignAgentsDTO(List.of());

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID_AGENTS, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.agentIds.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignAgents_shouldReturnNotFoundWhenGroupDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;
    AssignAgentsDTO assignDTO = new AssignAgentsDTO(List.of(testAgent1.getId()));

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID_AGENTS, nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignAgents_shouldReturnNotFoundWhenAgentDoesNotExist() throws Exception {
    String nonExistentAgentId = "550e8400-e29b-41d4-a716-446655440999";
    AssignAgentsDTO assignDTO = new AssignAgentsDTO(List.of(nonExistentAgentId));

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID_AGENTS, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignAgents_shouldReturnBadRequestWhenAgentIdsIsNull() throws Exception {
    AssignAgentsDTO assignDTO = new AssignAgentsDTO(null);

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID_AGENTS, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void assignAgents_shouldReturnForbiddenForNonAdminUser() throws Exception {
    AssignAgentsDTO assignDTO = new AssignAgentsDTO(List.of(testAgent1.getId()));

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID_AGENTS, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void assignAgents_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    AssignAgentsDTO assignDTO = new AssignAgentsDTO(List.of(testAgent1.getId()));

    mockMvc
        .perform(
            put(API_V1_GROUPS_ID_AGENTS, testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignDTO)))
        .andExpect(status().isUnauthorized());
  }
}
