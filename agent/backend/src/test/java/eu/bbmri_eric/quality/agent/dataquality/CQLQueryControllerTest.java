package eu.bbmri_eric.quality.agent.dataquality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.domain.CQLQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CQLQueryControllerTest {

  public static final String CQLEndpoint = "/api/cql-queries";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @WithUserDetails("admin")
  void post_validCQLQuery_createdAndRetrievable() throws Exception {
    CQLQuery check = new CQLQuery("Test CQL", "Checks patients with diabetes", "define Test: true");

    String location =
        mockMvc
            .perform(
                post(CQLEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(check)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();

    mockMvc
        .perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test CQL"))
        .andExpect(jsonPath("$.description").value("Checks patients with diabetes"))
        .andExpect(jsonPath("$.query").value("define Test: true"));
  }

  @Test
  @WithUserDetails("admin")
  void put_existingCQLQuery_updatedSuccessfully() throws Exception {
    CQLQuery check = new CQLQuery("UpdateTest", "Initial", "define Test: false");

    String location =
        mockMvc
            .perform(
                post(CQLEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(check)))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    check.setDescription("Updated Description");

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(check)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(location))
        .andExpect(jsonPath("$.description").value("Updated Description"));
  }

  @Test
  @WithUserDetails("admin")
  void delete_existingCQLQuery_deletedSuccessfully() throws Exception {
    CQLQuery check = new CQLQuery("DeleteTest", "To be deleted", "define Test: exists [Patient]");

    String location =
        mockMvc
            .perform(
                post(CQLEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(check)))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mockMvc.perform(delete(location)).andExpect(status().isNoContent());

    mockMvc.perform(get(location)).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void post_invalidCQLQuery_missingFields_returnsBadRequest() throws Exception {
    String invalidJson = "{\"name\": \"Invalid Query\"}";
    mockMvc
        .perform(post(CQLEndpoint).contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isConflict());
  }

  @Test
  @WithUserDetails("admin")
  void put_nonExistingCQLQuery_returnsNotFound() throws Exception {
    CQLQuery check = new CQLQuery(9999L, "Nonexistent", "No such ID", "define Test: false");

    mockMvc
        .perform(
            put(CQLEndpoint + "/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(check)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void delete_nonExistingCQLQuery_returnsNotFound() throws Exception {
    mockMvc.perform(delete(CQLEndpoint + "/9999")).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void get_malformedId_returnsBadRequest() throws Exception {
    mockMvc.perform(get(CQLEndpoint + "/abc")).andExpect(status().isBadRequest());
  }
}
