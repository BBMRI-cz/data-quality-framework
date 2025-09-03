package eu.bbmri_eric.quality.agent.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.user.TestUserSeedConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@Import(TestUserSeedConfig.class)
@AutoConfigureMockMvc()
class CQLQueryIntegrationTest {

  public static final String CQLEndpoint = "/api/cql-queries";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void post_validCQLQuery_createdAndRetrievable() throws Exception {
    CQLQuery check = new CQLQuery();
    check.setName("Test CQL");
    check.setDescription("Checks patients with diabetes");
    check.setQuery("define Test: true");

    String location =
        mockMvc
            .perform(
                post(CQLEndpoint)
                    .with(httpBasic("admin", "pass"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(check)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();

    mockMvc
        .perform(get(location).with(httpBasic("admin", "pass")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test CQL"))
        .andExpect(jsonPath("$.description").value("Checks patients with diabetes"))
        .andExpect(jsonPath("$.query").value("define Test: true"));
  }

  @Test
  void put_existingCQLQuery_updatedSuccessfully() throws Exception {
    CQLQuery check = new CQLQuery();
    check.setName("UpdateTest");
    check.setDescription("Initial");
    check.setQuery("define Test: false");

    String location =
        mockMvc
            .perform(
                post(CQLEndpoint)
                    .with(httpBasic("admin", "pass"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(check)))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    check.setDescription("Updated Description");

    mockMvc
        .perform(
            put(location)
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(check)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(location).with(httpBasic("admin", "pass")))
        .andExpect(jsonPath("$.description").value("Updated Description"));
  }

  @Test
  void delete_existingCQLQuery_deletedSuccessfully() throws Exception {
    CQLQuery check = new CQLQuery();
    check.setName("DeleteTest");
    check.setDescription("To be deleted");
    check.setQuery("define Test: exists [Patient]");

    String location =
        mockMvc
            .perform(
                post(CQLEndpoint)
                    .with(httpBasic("admin", "pass"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(check)))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mockMvc
        .perform(delete(location).with(httpBasic("admin", "pass")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(location).with(httpBasic("admin", "pass")))
        .andExpect(status().isNotFound());
  }

  @Test
  void post_invalidCQLQuery_missingFields_returnsBadRequest() throws Exception {
    CQLQuery invalidCheck = new CQLQuery();
    invalidCheck.setName("Invalid Query");

    mockMvc
        .perform(
            post(CQLEndpoint)
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCheck)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void put_nonExistingCQLQuery_returnsNotFound() throws Exception {
    CQLQuery check = new CQLQuery();
    check.setId(9999L);
    check.setName("Nonexistent");
    check.setDescription("No such ID");
    check.setQuery("define Test: false");

    mockMvc
        .perform(
            put(CQLEndpoint + "/99999")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(check)))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_nonExistingCQLQuery_returnsNotFound() throws Exception {
    mockMvc
        .perform(delete(CQLEndpoint + "/9999").with(httpBasic("admin", "pass")))
        .andExpect(status().isNotFound());
  }

  @Test
  void get_malformedId_returnsBadRequest() throws Exception {
    mockMvc
        .perform(get(CQLEndpoint + "/abc").with(httpBasic("admin", "pass")))
        .andExpect(status().isBadRequest());
  }
}
