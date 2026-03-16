package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.dto.KeywordDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class QualityCheckControllerTest {

  public static final String API_V1_QUALITY_CHECKS = "/api/v1/quality-checks";
  public static final String API_V1_QUALITY_CHECKS_ID = "/api/v1/quality-checks/{id}";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private QualityCheckRepository qualityCheckRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private EntityManager entityManager;

  private String testQualityCheckHash;
  private QualityCheck testQualityCheck;
  private Category testCategory;

  @BeforeEach
  void setUp() {

    testQualityCheckHash = "test-hash-" + UUID.randomUUID().toString().substring(0, 8);
    testQualityCheck =
        new QualityCheck(
            testQualityCheckHash,
            "Test Quality Check",
            "A test quality check for unit tests",
            0.8,
            0.5);
    testQualityCheck = qualityCheckRepository.save(testQualityCheck);

    testQualityCheck.addKeyword("gender");
    testQualityCheck.addKeyword("sex");
    testQualityCheck.addKeyword("male");
    qualityCheckRepository.save(testQualityCheck);
    testCategory = new Category("Data Completeness", "#FF5733");
    categoryRepository.save(testCategory);
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
            "Updated Quality Check", "Updated description for the quality check", 0.75, 0.45, null);

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
        new QualityCheckUpdateDTO("Updated Quality Check", "Updated description", 0.75, 0.45, null);

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
        new QualityCheckUpdateDTO("Updated Quality Check", "Updated description", 0.75, 0.45, null);

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
            0.45,
            null);

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
        new QualityCheckUpdateDTO("Updated Quality Check", "Updated description", 0.75, 0.45, null);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldAssignCategoryToQualityCheck() throws Exception {
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Updated Quality Check", "Updated description", 0.75, 0.45, testCategory.getId());

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.category.id").value(testCategory.getId()))
        .andExpect(jsonPath("$.category.name").value("Data Completeness"))
        .andExpect(jsonPath("$.category.colorHex").value("#FF5733"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldRemoveCategoryFromQualityCheckWhenCategoryIdIsNull() throws Exception {
    testQualityCheck.setCategory(testCategory);
    qualityCheckRepository.save(testQualityCheck);

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO("Updated Quality Check", "Updated description", 0.75, 0.45, null);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.category").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
    Long nonExistentCategoryId = 99999L;
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Updated Quality Check", "Updated description", 0.75, 0.45, nonExistentCategoryId);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldChangeCategoryAssignment() throws Exception {
    testQualityCheck.setCategory(testCategory);
    qualityCheckRepository.save(testQualityCheck);

    Category newCategory = new Category("Data Accuracy", "#00FF00");
    newCategory = categoryRepository.save(newCategory);

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Updated Quality Check", "Updated description", 0.75, 0.45, newCategory.getId());

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.category.id").value(newCategory.getId()))
        .andExpect(jsonPath("$.category.name").value("Data Accuracy"))
        .andExpect(jsonPath("$.category.colorHex").value("#00FF00"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_category_shouldSetCategoryToNullForAssociatedQualityChecks() throws Exception {
    testQualityCheck.setCategory(testCategory);
    qualityCheckRepository.save(testQualityCheck);
    entityManager.flush();

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category.id").value(testCategory.getId()));

    Long categoryId = testCategory.getId();
    entityManager.clear();
    categoryRepository.deleteById(categoryId);
    entityManager.flush();
    entityManager.clear();

    assertFalse(categoryRepository.existsById(categoryId));

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.category").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldReturnKeywordsWhenAdded() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheckHash))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords").isArray())
        .andExpect(jsonPath("$.keywords.length()").value(3))
        .andExpect(jsonPath("$.keywords", hasItems("gender", "sex", "male")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findAll_shouldReturnKeywordsForAllQualityChecks() throws Exception {
    QualityCheck secondQualityCheck =
        new QualityCheck(
            "second-hash", "Second Quality Check", "Another test quality check", 0.9, 0.6);
    secondQualityCheck.addKeyword("diagnosis");
    secondQualityCheck.addKeyword("C50");
    qualityCheckRepository.save(testQualityCheck);
    qualityCheckRepository.save(secondQualityCheck);

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.qualityChecks").isArray())
        .andExpect(jsonPath("$._embedded.qualityChecks.length()").value(2))
        .andExpect(jsonPath("$._embedded.qualityChecks[1].keywords", hasItems("C50", "diagnosis")))
        .andExpect(
            jsonPath("$._embedded.qualityChecks[0].keywords", hasItems("gender", "sex", "male")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void addKeyword_shouldAddKeywordToQualityCheckAndReturnHateoasResponse() throws Exception {
    KeywordDTO keywordDTO = new KeywordDTO("patient data");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            post(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.keywords").isArray())
        .andExpect(jsonPath("$.keywords.length()").value(4))
        .andExpect(jsonPath("$.keywords", hasItems("gender", "sex", "male", "patient data")))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheckHash))
        .andExpect(
            jsonPath("$._links.quality-checks.href")
                .value("http://localhost/api/v1/quality-checks"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void addKeyword_shouldReturnForbiddenForNonAdminUser() throws Exception {
    KeywordDTO keywordDTO = new KeywordDTO("test keyword");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            post(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void addKeyword_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    String nonExistentHash = "non-existent-hash";
    KeywordDTO keywordDTO = new KeywordDTO("test keyword");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            post(keywordsEndpoint, nonExistentHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void addKeyword_shouldReturnBadRequestForInvalidData() throws Exception {
    KeywordDTO keywordDTO = new KeywordDTO("");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            post(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void addKeyword_shouldReturnBadRequestForKeywordTooLong() throws Exception {
    String longKeyword = "a".repeat(251);
    KeywordDTO keywordDTO = new KeywordDTO(longKeyword);
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            post(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addKeyword_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    KeywordDTO keywordDTO = new KeywordDTO("test keyword");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            post(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void removeKeyword_shouldRemoveKeywordFromQualityCheckAndReturnHateoasResponse()
      throws Exception {
    KeywordDTO keywordDTO = new KeywordDTO("gender");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            delete(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.keywords").isArray())
        .andExpect(jsonPath("$.keywords.length()").value(2))
        .andExpect(jsonPath("$.keywords", hasItems("sex", "male")))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheckHash))
        .andExpect(
            jsonPath("$._links.quality-checks.href")
                .value("http://localhost/api/v1/quality-checks"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void removeKeyword_shouldReturnForbiddenForNonAdminUser() throws Exception {
    KeywordDTO keywordDTO = new KeywordDTO("gender");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            delete(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void removeKeyword_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    String nonExistentHash = "non-existent-hash";
    KeywordDTO keywordDTO = new KeywordDTO("test keyword");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            delete(keywordsEndpoint, nonExistentHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void removeKeyword_shouldReturnNotFoundWhenKeywordDoesNotExist() throws Exception {
    KeywordDTO keywordDTO = new KeywordDTO("non-existent-keyword");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            delete(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  void removeKeyword_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    KeywordDTO keywordDTO = new KeywordDTO("gender");
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            delete(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordDTO)))
        .andExpect(status().isUnauthorized());
  }
}
