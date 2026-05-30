package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.AgentInteractionType;
import eu.bbmri_eric.quality.server.dataquality.domain.AgentStatus;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentUpdateRequest;
import eu.bbmri_eric.quality.server.user.UserCreateDTO;
import eu.bbmri_eric.quality.server.user.UserRepository;
import eu.bbmri_eric.quality.server.user.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgentControllerIntegrationTest {
  public static final String API_V_1_AGENTS = "/api/v1/agents";
  public static final String API_V_1_AGENTS_ID = "/api/v1/agents/{id}";
  public static final String API_V1_AGENTS_REPORTS = "/api/v1/agents/{agentId}/reports";
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Autowired private AgentRepository agentRepository;

  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    agentRepository.deleteAll();
  }

  @Test
  void create_shouldCreateAgentAndReturnHateoasResponse() throws Exception {
    String agentId = UUID.randomUUID().toString();
    AgentRegistrationRequest createDto = new AgentRegistrationRequest(agentId);
    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.agent.id").value(agentId));
    assertTrue(agentRepository.findById(agentId).isPresent());
  }

  @Test
  void create_shouldReturnBadRequestForInvalidUuid() throws Exception {
    AgentRegistrationRequest createDto = new AgentRegistrationRequest("invalid-uuid");
    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void create_shouldReturnBadRequestForBlankId() throws Exception {
    AgentRegistrationRequest createDto = new AgentRegistrationRequest("");
    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void create_shouldReturnBadRequestForNullId() throws Exception {
    AgentRegistrationRequest createDto = new AgentRegistrationRequest(null);

    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void findById_shouldReturnAgentWithHateoasLinksWhenExists() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId))
        .andExpect(
            jsonPath("$._links.self.href").value("http://localhost/api/v1/agents/" + agentId))
        .andExpect(jsonPath("$._links.agents.href").value("http://localhost/api/v1/agents"));
  }

  @Test
  void findById_shouldReturnOkWhenAgentUserChecksOwnStatus() throws Exception {
    String agentId = UUID.randomUUID().toString();
    agentRepository.save(new Agent(agentId));
    userService.createUser(new UserCreateDTO("agent-" + agentId, agentId));

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId).with(user("agent-" + agentId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldReturnNotFoundWhenAgentDoesNotExist() throws Exception {
    String nonExistentAgentId = UUID.randomUUID().toString();
    mockMvc.perform(get(API_V_1_AGENTS_ID, nonExistentAgentId)).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listAll_shouldReturnEmptyListWithHateoasLinksWhenNoAgents() throws Exception {
    mockMvc
        .perform(get(API_V_1_AGENTS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/agents"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listAll_shouldReturnAllAgentsWithHateoasLinks() throws Exception {
    String agentId1 = UUID.randomUUID().toString();
    String agentId2 = UUID.randomUUID().toString();
    agentRepository.save(new Agent(agentId1));
    agentRepository.save(new Agent(agentId2));
    mockMvc
        .perform(get(API_V_1_AGENTS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.agents").isArray())
        .andExpect(jsonPath("$._embedded.agents.length()").value(2))
        .andExpect(jsonPath("$._embedded.agents[?(@.id == '" + agentId1 + "')]").exists())
        .andExpect(jsonPath("$._embedded.agents[?(@.id == '" + agentId2 + "')]").exists())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/agents"))
        .andExpect(jsonPath("$._embedded.agents[0]._links.self.href").exists())
        .andExpect(jsonPath("$._embedded.agents[1]._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = {})
  void listAll_shouldReturnForbiddenWhenUserHasNoRole() throws Exception {
    mockMvc.perform(get(API_V_1_AGENTS)).andExpect(status().isForbidden());
  }

  @Test
  void create_shouldHandleDuplicateAgentId() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent existingAgent = new Agent(agentId);
    agentRepository.save(existingAgent);
    AgentRegistrationRequest createDto = new AgentRegistrationRequest(agentId);
    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isConflict());
  }

  @Test
  void create_shouldReturnTooManyRequestsForDuplicateIpAddress() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent existingAgent = new Agent(agentId, "192.168.1.1");
    agentRepository.save(existingAgent);
    AgentRegistrationRequest createDto = new AgentRegistrationRequest(UUID.randomUUID().toString());
    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .with(
                    request -> {
                      request.setRemoteAddr("192.168.1.1");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  @WithUserDetails("admin")
  void endToEndFlow_createAndRetrieveAgent() throws Exception {
    String agentId = UUID.randomUUID().toString();
    AgentRegistrationRequest createDto = new AgentRegistrationRequest(agentId);

    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId));

    mockMvc
        .perform(get(API_V_1_AGENTS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.agents[?(@.id == '" + agentId + "')]").exists());
  }

  @Test
  @WithUserDetails("admin")
  void update_shouldUpdateAgentNameAndReturnHateoasResponse() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);

    AgentUpdateRequest updateRequest = new AgentUpdateRequest("Updated Agent Name", null);

    mockMvc
        .perform(
            patch(API_V_1_AGENTS_ID, agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId))
        .andExpect(jsonPath("$.name").value("Updated Agent Name"))
        .andExpect(
            jsonPath("$._links.self.href").value("http://localhost/api/v1/agents/" + agentId))
        .andExpect(jsonPath("$._links.agents.href").value("http://localhost/api/v1/agents"));
  }

  @Test
  @WithUserDetails("admin")
  void update_shouldUpdateAgentStatusAndReturnHateoasResponse() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);

    AgentUpdateRequest updateRequest = new AgentUpdateRequest(null, AgentStatus.ACTIVE);

    mockMvc
        .perform(
            patch(API_V_1_AGENTS_ID, agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(
            jsonPath("$._links.self.href").value("http://localhost/api/v1/agents/" + agentId));
  }

  @Test
  @WithUserDetails("admin")
  void update_shouldUpdateBothNameAndStatus() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);

    AgentUpdateRequest updateRequest = new AgentUpdateRequest("Test Agent", AgentStatus.INACTIVE);

    mockMvc
        .perform(
            patch(API_V_1_AGENTS_ID, agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId))
        .andExpect(jsonPath("$.name").value("Test Agent"))
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnNotFoundWhenAgentDoesNotExist() throws Exception {
    String nonExistentAgentId = UUID.randomUUID().toString();
    AgentUpdateRequest updateRequest = new AgentUpdateRequest("New Name", AgentStatus.ACTIVE);

    mockMvc
        .perform(
            patch(API_V_1_AGENTS_ID, nonExistentAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void update_shouldHandleEmptyUpdateRequest() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);

    AgentUpdateRequest updateRequest = new AgentUpdateRequest(null, null);

    mockMvc
        .perform(
            patch(API_V_1_AGENTS_ID, agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId));
  }

  @Test
  void update_shouldRequireAuthentication() throws Exception {
    String agentId = UUID.randomUUID().toString();
    AgentUpdateRequest updateRequest = new AgentUpdateRequest("New Name", AgentStatus.ACTIVE);
    mockMvc
        .perform(
            patch(API_V_1_AGENTS_ID, agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("admin")
  void endToEndFlow_createUpdateAndRetrieveAgent() throws Exception {
    String agentId = UUID.randomUUID().toString();
    AgentRegistrationRequest createDto = new AgentRegistrationRequest(agentId);

    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isCreated());

    AgentUpdateRequest updateRequest = new AgentUpdateRequest("Updated Agent", AgentStatus.ACTIVE);
    mockMvc
        .perform(
            patch(API_V_1_AGENTS_ID, agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Agent"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId))
        .andExpect(jsonPath("$.name").value("Updated Agent"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @WithUserDetails("admin")
  void findById_shouldNotIncludeInteractionsByDefault() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agent.addInteraction(AgentInteractionType.PING);
    agent.addInteraction(AgentInteractionType.REPORT);
    agentRepository.save(agent);

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId))
        .andExpect(jsonPath("$.interactions").doesNotExist());
  }

  @Test
  @WithUserDetails("admin")
  void findById_shouldIncludeInteractionsWhenExpandParameterProvided() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agent.addInteraction(AgentInteractionType.PING);
    agent.addInteraction(AgentInteractionType.REPORT);
    agentRepository.save(agent);

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId).param("expand", "interactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId))
        .andExpect(jsonPath("$.interactions").isArray())
        .andExpect(jsonPath("$.interactions.length()").value(3))
        .andExpect(jsonPath("$.interactions[0].type").value("REPORT"))
        .andExpect(jsonPath("$.interactions[0].id").exists())
        .andExpect(jsonPath("$.interactions[0].timestamp").exists())
        .andExpect(jsonPath("$.interactions[1].type").value("PING"))
        .andExpect(jsonPath("$.interactions[1].id").exists())
        .andExpect(jsonPath("$.interactions[1].timestamp").exists());
  }

  @Test
  @WithUserDetails("admin")
  void findById_shouldIgnoreInvalidExpandParameter() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agent.addInteraction(AgentInteractionType.PING);
    agentRepository.save(agent);

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId).param("expand", "invalid"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(agentId))
        .andExpect(jsonPath("$.interactions").doesNotExist());
  }

  @Test
  @WithUserDetails("admin")
  void findById_shouldReturnAtMost30MostRecentPingInteractions_whenExpandIsRequested()
      throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    for (int i = 0; i < 30; i++) {
      agent.addInteraction(AgentInteractionType.PING);
    }
    agentRepository.save(agent);

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId).param("expand", "interactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interactions").isArray())
        .andExpect(jsonPath("$.interactions.length()").value(31));

    agent.addInteraction(AgentInteractionType.PING);
    agentRepository.save(agent);

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId).param("expand", "interactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interactions").isArray())
        .andExpect(jsonPath("$.interactions.length()").value(31));

    long pingCount =
        agentRepository.findById(agentId).orElseThrow().getInteractions().stream()
            .filter(i -> i.getType() == AgentInteractionType.PING)
            .count();
    assertEquals(30, pingCount);
  }

  @Test
  @WithUserDetails("admin")
  void findById_shouldPreserveNonPingInteractions_whenPingLimitExceeded() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    for (int i = 0; i < 30; i++) {
      agent.addInteraction(AgentInteractionType.PING);
    }
    agent.addInteraction(AgentInteractionType.REPORT);
    agent.addInteraction(AgentInteractionType.REPORT);
    agent.setVersion("1.0.0");
    agentRepository.save(agent);

    mockMvc
        .perform(get(API_V_1_AGENTS_ID, agentId).param("expand", "interactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interactions").isArray())
        .andExpect(jsonPath("$.interactions.length()").value(34));

    Agent savedAgent = agentRepository.findById(agentId).orElseThrow();
    long pingCount =
        savedAgent.getInteractions().stream()
            .filter(i -> i.getType() == AgentInteractionType.PING)
            .count();
    long reportCount =
        savedAgent.getInteractions().stream()
            .filter(i -> i.getType() == AgentInteractionType.REPORT)
            .count();
    long versionUpdateCount =
        savedAgent.getInteractions().stream()
            .filter(i -> i.getType() == AgentInteractionType.VERSION_UPDATE)
            .count();
    long registrationCount =
        savedAgent.getInteractions().stream()
            .filter(i -> i.getType() == AgentInteractionType.REGISTRATION)
            .count();

    assertEquals(30, pingCount);
    assertEquals(2, reportCount);
    assertEquals(1, versionUpdateCount);
    assertEquals(1, registrationCount);
  }

  @Test
  @WithUserDetails("admin")
  void delete_shouldDeleteAgentWhenUserIsAdmin() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);
    userService.createUser(new UserCreateDTO("agent-" + agentId, agentId));

    assertTrue(agentRepository.findById(agentId).isPresent());

    mockMvc.perform(delete(API_V_1_AGENTS_ID, agentId)).andExpect(status().isNoContent());

    assertTrue(agentRepository.findById(agentId).isEmpty());
    assertTrue(userRepository.findByAgentId(agentId).isEmpty());
  }

  @Test
  @WithUserDetails("admin")
  void delete_shouldDeleteAgentInteractionsAndReportsWhenAgentIsDeleted() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agent.addInteraction(AgentInteractionType.PING);
    agent.addInteraction(AgentInteractionType.REPORT);
    agent.addReport(new Report());
    agentRepository.save(agent);
    userService.createUser(new UserCreateDTO("agent-" + agentId, agentId));

    Agent savedAgent = agentRepository.findById(agentId).orElseThrow();
    assertEquals(3, savedAgent.getInteractions().size()); // REGISTRATION + PING + REPORT

    mockMvc.perform(delete(API_V_1_AGENTS_ID, agentId)).andExpect(status().isNoContent());
    mockMvc.perform(get(API_V1_AGENTS_REPORTS, agentId)).andExpect(status().isNotFound());
    assertTrue(agentRepository.findById(agentId).isEmpty());
    assertTrue(userRepository.findByAgentId(agentId).isEmpty());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void delete_shouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);

    mockMvc.perform(delete(API_V_1_AGENTS_ID, agentId)).andExpect(status().isForbidden());

    assertTrue(agentRepository.findById(agentId).isPresent());
  }

  @Test
  void delete_shouldRequireAuthentication() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);

    mockMvc.perform(delete(API_V_1_AGENTS_ID, agentId)).andExpect(status().isUnauthorized());

    assertTrue(agentRepository.findById(agentId).isPresent());
  }

  @Test
  @WithUserDetails("admin")
  void delete_shouldReturnNotFoundWhenAgentDoesNotExist() throws Exception {
    String nonExistentAgentId = UUID.randomUUID().toString();

    mockMvc.perform(delete(API_V_1_AGENTS_ID, nonExistentAgentId)).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void delete_shouldBeIdempotent() throws Exception {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    agentRepository.save(agent);
    userService.createUser(new UserCreateDTO("agent-" + agentId, agentId));

    mockMvc.perform(delete(API_V_1_AGENTS_ID, agentId)).andExpect(status().isNoContent());
    assertTrue(agentRepository.findById(agentId).isEmpty());
    assertTrue(userRepository.findByAgentId(agentId).isEmpty());

    mockMvc.perform(delete(API_V_1_AGENTS_ID, agentId)).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void endToEndFlow_createUpdateAndDeleteAgent() throws Exception {
    String agentId = UUID.randomUUID().toString();
    AgentRegistrationRequest createDto = new AgentRegistrationRequest(agentId);

    mockMvc
        .perform(
            post(API_V_1_AGENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isCreated());

    assertTrue(agentRepository.findById(agentId).isPresent());

    AgentUpdateRequest updateRequest = new AgentUpdateRequest("Test Agent", AgentStatus.ACTIVE);
    mockMvc
        .perform(
            patch(API_V_1_AGENTS_ID, agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk());

    mockMvc.perform(delete(API_V_1_AGENTS_ID, agentId)).andExpect(status().isNoContent());

    assertTrue(agentRepository.findById(agentId).isEmpty());
    assertTrue(userRepository.findByAgentId(agentId).isEmpty());
  }

  @Test
  @WithUserDetails("admin")
  void delete_shouldDeleteAssociatedUserWhenAgentIsDeleted() throws Exception {
    String agentId = UUID.randomUUID().toString();
    agentRepository.save(new Agent(agentId));
    userService.createUser(new UserCreateDTO("agent-" + agentId, agentId));

    assertTrue(agentRepository.findById(agentId).isPresent());
    assertTrue(userRepository.findByAgentId(agentId).isPresent());

    mockMvc.perform(delete(API_V_1_AGENTS_ID, agentId)).andExpect(status().isNoContent());

    assertTrue(agentRepository.findById(agentId).isEmpty());
    assertTrue(userRepository.findByAgentId(agentId).isEmpty());
  }
}
