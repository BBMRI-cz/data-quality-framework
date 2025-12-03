package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.server.dto.ServerCreateDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerUpdateDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ServerControllerTest {

  private static final String SERVERS_ENDPOINT = "/api/servers";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ServerRepository serverRepository;

  @AfterEach
  void tearDown() {
    serverRepository.deleteAll();
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
}
