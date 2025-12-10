package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QualityCheckControllerTest {

  public static final String API_V1_QUALITY_CHECKS = "/api/v1/quality-checks";
  public static final String API_V1_QUALITY_CHECKS_ID = "/api/v1/quality-checks/{id}";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private QualityCheckRepository qualityCheckRepository;

  private String testQualityCheckHash;
  private QualityCheck testQualityCheck;

  @BeforeEach
  void setUp() {
    qualityCheckRepository.deleteAll();
    testQualityCheckHash = "test-hash-" + UUID.randomUUID().toString().substring(0, 8);
    testQualityCheck =
        new QualityCheck(
            testQualityCheckHash,
            "Test Quality Check",
            "A test quality check for unit tests",
            0.8,
            0.5);
    qualityCheckRepository.save(testQualityCheck);
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnQualityCheckWithHateoasLinksWhenExists() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.name").value("Test Quality Check"))
        .andExpect(jsonPath("$.description").value("A test quality check for unit tests"))
        .andExpect(jsonPath("$.warningThreshold").value(0.8))
        .andExpect(jsonPath("$.errorThreshold").value(0.5))
        .andExpect(jsonPath("$.registeredAt").exists())
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheckHash))
        .andExpect(
            jsonPath("$._links.quality-checks.href")
                .value("http://localhost/api/v1/quality-checks"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    String nonExistentHash = "non-existent-hash";

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, nonExistentHash))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldReturnQualityCheckForAdmin() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnEmptyListWithHateoasLinksWhenNoQualityChecks() throws Exception {
    qualityCheckRepository.deleteAll();

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/quality-checks"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnAllQualityChecksWithHateoasLinks() throws Exception {
    QualityCheck secondQualityCheck =
        new QualityCheck(
            "second-hash", "Second Quality Check", "Another test quality check", 0.9, 0.6);
    qualityCheckRepository.save(secondQualityCheck);

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.qualityChecks").isArray())
        .andExpect(jsonPath("$._embedded.qualityChecks.length()").value(2))
        .andExpect(
            jsonPath("$._embedded.qualityChecks[?(@.hash == '" + testQualityCheckHash + "')]")
                .exists())
        .andExpect(jsonPath("$._embedded.qualityChecks[?(@.hash == 'second-hash')]").exists())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/quality-checks"))
        .andExpect(jsonPath("$._embedded.qualityChecks[0]._links.self.href").exists())
        .andExpect(jsonPath("$._embedded.qualityChecks[1]._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findAll_shouldReturnAllQualityChecksForAdmin() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.qualityChecks").isArray())
        .andExpect(jsonPath("$._embedded.qualityChecks.length()").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldUpdateQualityCheckAndReturnHateoasResponse() throws Exception {
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Updated Quality Check", "Updated description for the quality check", 0.75, 0.45);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.name").value("Updated Quality Check"))
        .andExpect(jsonPath("$.description").value("Updated description for the quality check"))
        .andExpect(jsonPath("$.warningThreshold").value(0.75))
        .andExpect(jsonPath("$.errorThreshold").value(0.45))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheckHash))
        .andExpect(
            jsonPath("$._links.quality-checks.href")
                .value("http://localhost/api/v1/quality-checks"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void update_shouldReturnForbiddenForNonAdminUser() throws Exception {
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO("Updated Quality Check", "Updated description", 0.75, 0.45);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    String nonExistentHash = "non-existent-hash";
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO("Updated Quality Check", "Updated description", 0.75, 0.45);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, nonExistentHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnBadRequestForInvalidData() throws Exception {
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "", // empty name should trigger validation error
            "Updated description",
            0.75,
            0.45);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findById_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void findAll_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc.perform(get(API_V1_QUALITY_CHECKS)).andExpect(status().isUnauthorized());
  }

  @Test
  void update_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO("Updated Quality Check", "Updated description", 0.75, 0.45);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isUnauthorized());
  }
}
