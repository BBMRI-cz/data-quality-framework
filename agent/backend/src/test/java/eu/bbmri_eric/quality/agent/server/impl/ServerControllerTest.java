package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.RegistrationCredentials;
import eu.bbmri_eric.quality.agent.server.domain.RegistrationStatus;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.dto.ServerCreateDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ServerControllerTest {

  private static final String SERVERS_ENDPOINT = "/api/servers";

  @Autowired private MockMvc mockMvc;

  @Autowired private JsonMapper objectMapper;

  @Autowired private ServerRepository serverRepository;

  @MockitoBean private CentralServerClientFactory clientFactory;

  private final CentralServerClient client = mock(CentralServerClient.class);

  @BeforeEach
  void setUp() {
    when(clientFactory.createClient(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(client);
    when(client.register()).thenReturn(new RegistrationCredentials("client-id", "client-secret"));
  }

  @Test
  @WithUserDetails("admin")
  void getAllServers_returnsEmptyList() throws Exception {
    mockMvc
        .perform(get(SERVERS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$._links.self").exists());
  }

  @Test
  @WithUserDetails("admin")
  void createServer_validData_createdSuccessfully() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.url").value("https://example.com"))
            .andExpect(jsonPath("$.name").value("Test Server"))
            .andExpect(jsonPath("$._links.self").exists())
            .andExpect(jsonPath("$._links.servers").exists())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();
  }

  @Test
  @WithUserDetails("admin")
  void createServer_invalidUrl_returnsBadRequest() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("invalid-url");
    createDto.setName("Test Server");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void createServer_emptyUrl_returnsBadRequest() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("");
    createDto.setName("Test Server");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void createServer_emptyName_returnsBadRequest() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void createServer_duplicateUrl_returnsConflict() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isCreated());

    ServerCreateDto duplicateDto = new ServerCreateDto();
    duplicateDto.setUrl("https://example.com");
    duplicateDto.setName("Duplicate Server");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateDto)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithUserDetails("admin")
  void createServer_withPublicKey_createdSuccessfully() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");
    createDto.setPublicKey(
        "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\n-----END PUBLIC KEY-----");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.url").value("https://example.com"))
        .andExpect(jsonPath("$.publicKey").value(createDto.getPublicKey()));
  }

  @Test
  @WithUserDetails("admin")
  void createServer_withoutPublicKey_hasNullPublicKey() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicKey").doesNotExist());
  }

  @Test
  @WithUserDetails("admin")
  void createServer_publicKeyTooLong_returnsBadRequest() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");
    createDto.setPublicKey("x".repeat(2049));

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void updateServer_addPublicKeyAfterRegistration_addedSuccessfully() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    ServerUpdateDto updateDto = new ServerUpdateDto();
    updateDto.setPublicKey(
        "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\n-----END PUBLIC KEY-----");

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicKey").value(updateDto.getPublicKey()));

    mockMvc
        .perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicKey").value(updateDto.getPublicKey()));
  }

  @Test
  @WithUserDetails("admin")
  void updateServer_publicKeyTooLong_returnsBadRequest() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    ServerUpdateDto updateDto = new ServerUpdateDto();
    updateDto.setPublicKey("x".repeat(2049));

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void getServerById_existingServer_returnsServer() throws Exception {
    // Create a server first
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    // Retrieve the server
    mockMvc
        .perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value("https://example.com"))
        .andExpect(jsonPath("$.name").value("Test Server"))
        .andExpect(jsonPath("$._links.self").exists())
        .andExpect(jsonPath("$._links.servers").exists());
  }

  @Test
  @WithUserDetails("admin")
  void getServerById_nonExistingServer_returnsNotFound() throws Exception {
    mockMvc.perform(get(SERVERS_ENDPOINT + "/non-existing-id")).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void updateServer_validData_updatedSuccessfully() throws Exception {
    // Create a server first
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Original Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    // Update the server
    ServerUpdateDto updateDto = new ServerUpdateDto();
    updateDto.setUrl("https://updated.example.com");
    updateDto.setName("Updated Server");

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value("https://updated.example.com"))
        .andExpect(jsonPath("$.name").value("Updated Server"))
        .andExpect(jsonPath("$._links.self").exists())
        .andExpect(jsonPath("$._links.servers").exists());
  }

  @Test
  @WithUserDetails("admin")
  void updateServer_invalidUrl_returnsBadRequest() throws Exception {
    // Create a server first
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Original Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    // Update with invalid URL
    ServerUpdateDto updateDto = new ServerUpdateDto();
    updateDto.setUrl("invalid-url");
    updateDto.setName("Updated Server");

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void updateServer_duplicateUrl_returnsConflict() throws Exception {
    ServerCreateDto firstServer = new ServerCreateDto();
    firstServer.setUrl("https://first.example.com");
    firstServer.setName("First Server");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstServer)))
        .andExpect(status().isCreated());

    ServerCreateDto secondServer = new ServerCreateDto();
    secondServer.setUrl("https://second.example.com");
    secondServer.setName("Second Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondServer)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    ServerUpdateDto updateDto = new ServerUpdateDto();
    updateDto.setUrl("https://first.example.com");

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithUserDetails("admin")
  void updateServer_nonExistingServer_returnsNotFound() throws Exception {
    ServerUpdateDto updateDto = new ServerUpdateDto();
    updateDto.setUrl("https://example.com");
    updateDto.setName("Updated Server");

    mockMvc
        .perform(
            put(SERVERS_ENDPOINT + "/non-existing-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void deleteServer_existingServer_deletedSuccessfully() throws Exception {
    // Create a server first
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Server to Delete");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    mockMvc.perform(delete(location)).andExpect(status().isNoContent());
    mockMvc.perform(get(location)).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void deleteServer_nonExistingServer_returnsNotFound() throws Exception {
    mockMvc.perform(delete(SERVERS_ENDPOINT + "/non-existing-id")).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void getAllServers_withExistingServers_returnsServerList() throws Exception {
    // Create multiple servers
    ServerCreateDto server1 = new ServerCreateDto();
    server1.setUrl("https://server1.com");
    server1.setName("Server 1");

    ServerCreateDto server2 = new ServerCreateDto();
    server2.setUrl("https://server2.com");
    server2.setName("Server 2");

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(server1)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(SERVERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(server2)))
        .andExpect(status().isCreated());

    // Get all servers
    mockMvc
        .perform(get(SERVERS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.servers").isArray())
        .andExpect(jsonPath("$._embedded.servers").isNotEmpty())
        .andExpect(jsonPath("$._links.self").exists());
  }

  @Test
  @WithUserDetails("admin")
  void retryRegistration_serverWithoutCredentials_accepted() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mockMvc.perform(post(location + "/registration")).andExpect(status().isAccepted());
  }

  @Test
  @WithUserDetails("admin")
  void retryRegistration_nonExistingServer_returnsNotFound() throws Exception {
    mockMvc
        .perform(post(SERVERS_ENDPOINT + "/non-existing-id/registration"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void retryRegistration_serverWithCredentials_returnsConflict() throws Exception {
    ServerCreateDto createDto = new ServerCreateDto();
    createDto.setUrl("https://example.com");
    createDto.setName("Test Server");

    String location =
        mockMvc
            .perform(
                post(SERVERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    ServerUpdateDto updateDto = new ServerUpdateDto();
    updateDto.setClientId("client-1");
    updateDto.setClientSecret("secret-1");

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk());

    mockMvc.perform(post(location + "/registration")).andExpect(status().isConflict());
  }

  @Test
  @WithUserDetails("admin")
  void getRegistration_existingServer_returnsStateWithLinks() throws Exception {
    Server server = serverRepository.save(new Server("https://reg.example.com", "Reg Server"));
    server.setRegistrationStatus(RegistrationStatus.REGISTERING);
    server.setRegistrationRetryCount(1);
    server.setNextRegistrationRetryAt(java.time.LocalDateTime.now().plusMinutes(15));

    mockMvc
        .perform(get(SERVERS_ENDPOINT + "/" + server.getId() + "/registration"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REGISTERING"))
        .andExpect(jsonPath("$.attempts").value(1))
        .andExpect(jsonPath("$.nextRetryAt").exists())
        .andExpect(jsonPath("$._links.self.href", endsWith("/registration")))
        .andExpect(
            jsonPath("$._links.server.href", endsWith(SERVERS_ENDPOINT + "/" + server.getId())));
  }

  @Test
  @WithUserDetails("admin")
  void getRegistration_activeApprovedServer_derivesActiveStatus() throws Exception {
    Server server = serverRepository.save(new Server("https://reg-active.example.com", "Reg A"));
    server.setRegistrationStatus(RegistrationStatus.REGISTERED_PENDING_APPROVAL);
    server.setStatus(ServerConnectionStatus.ACTIVE);

    mockMvc
        .perform(get(SERVERS_ENDPOINT + "/" + server.getId() + "/registration"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    mockMvc
        .perform(get(SERVERS_ENDPOINT + "/" + server.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.registration.status").value("ACTIVE"));
  }

  @Test
  @WithUserDetails("admin")
  void getRegistration_unknownServer_returnsNotFound() throws Exception {
    mockMvc
        .perform(get(SERVERS_ENDPOINT + "/non-existing-id/registration"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithAnonymousUser
  void getRegistration_anonymous_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(get(SERVERS_ENDPOINT + "/any-id/registration"))
        .andExpect(status().isUnauthorized());
  }
}
