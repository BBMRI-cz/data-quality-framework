package eu.bbmri_eric.quality.agent.dataquality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class QualityCheckControllerTest {

  public static final String QualityCheckEndpoint = "/api/quality-checks";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @WithUserDetails("admin")
  void post_validQualityCheck_createdAndRetrievable() throws Exception {
    QualityCheck check =
        new QualityCheck("Test Check", "Checks patients with diabetes", "define Test: true");

    String location =
        mockMvc
            .perform(
                post(QualityCheckEndpoint)
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
        .andExpect(jsonPath("$.name").value("Test Check"))
        .andExpect(jsonPath("$.description").value("Checks patients with diabetes"))
        .andExpect(jsonPath("$.query").value("define Test: true"));
  }

  @Test
  @WithUserDetails("admin")
  void put_existingQualityCheck_updatedSuccessfully() throws Exception {
    QualityCheck check = new QualityCheck("UpdateTest", "Initial", "define Test: false");

    String location =
        mockMvc
            .perform(
                post(QualityCheckEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(check)))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();

    check.setDescription("Updated Description");

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(check)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get(location))
        .andExpect(jsonPath("$.description").value("Updated Description"));
  }

  @Test
  @WithUserDetails("admin")
  void delete_existingQualityCheck_deletedSuccessfully() throws Exception {
    QualityCheck check =
        new QualityCheck("DeleteTest", "To be deleted", "define Test: exists [Patient]");

    String location =
        mockMvc
            .perform(
                post(QualityCheckEndpoint)
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
  void post_invalidQualityCheck_missingFields_returnsBadRequest() throws Exception {
    String invalidJson = "{\"name\": \"Invalid Query\"}";
    mockMvc
        .perform(
            post(QualityCheckEndpoint).contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void put_nonExistingQualityCheck_returnsNotFound() throws Exception {
    QualityCheck check = new QualityCheck(9999L, "Nonexistent", "No such ID", "define Test: false");

    mockMvc
        .perform(
            put(QualityCheckEndpoint + "/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(check)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void delete_nonExistingQualityCheck_returnsNotFound() throws Exception {
    mockMvc.perform(delete(QualityCheckEndpoint + "/9999")).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void get_malformedId_returnsBadRequest() throws Exception {
    mockMvc.perform(get(QualityCheckEndpoint + "/abc")).andExpect(status().isBadRequest());
  }
}
