package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.dto.KeywordsDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionCreateDTO;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
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
  public static final String API_V1_QUALITY_CHECKS_VERSIONS =
      "/api/v1/quality-checks/{id}/versions";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private QualityCheckRepository qualityCheckRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private EntityManager entityManager;

  private QualityCheck testQualityCheck;
  private Category testCategory;

  @BeforeEach
  void setUp() {

    testQualityCheck =
        new QualityCheck("Test Quality Check", "A test quality check for unit tests", 0.8, 0.5);
    testQualityCheck = qualityCheckRepository.save(testQualityCheck);

    testQualityCheck.setKeywords(Set.of("gender", "sex", "male"));
    qualityCheckRepository.save(testQualityCheck);
    testCategory = new Category("Data Completeness", "#FF5733");
    categoryRepository.save(testCategory);
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnQualityCheckWithHateoasLinksWhenExists() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Quality Check"))
        .andExpect(jsonPath("$.description").value("A test quality check for unit tests"))
        .andExpect(jsonPath("$.warningThreshold").value(0.8))
        .andExpect(jsonPath("$.errorThreshold").value(0.5))
        .andExpect(jsonPath("$.registeredAt").exists())
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheck.getId()))
        .andExpect(
            jsonPath("$._links.quality-checks.href")
                .value("http://localhost/api/v1/quality-checks"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;

    mockMvc.perform(get(API_V1_QUALITY_CHECKS_ID, nonExistentId)).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldReturnQualityCheckForAdmin() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnEmptyVersionsWhenNoneExist() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versions").isArray())
        .andExpect(jsonPath("$.versions.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnVersionsWhenPresent() throws Exception {
    String query = "SELECT COUNT(*) FROM patients";
    testQualityCheck.addVersion(new QualityCheckVersion(testQualityCheck, 1, query));
    qualityCheckRepository.save(testQualityCheck);
    entityManager.flush();

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versions").isArray())
        .andExpect(jsonPath("$.versions.length()").value(1))
        .andExpect(jsonPath("$.versions[0].version").value(1))
        .andExpect(jsonPath("$.versions[0].query").value(query))
        .andExpect(jsonPath("$.versions[0].hash").value(hashOf(query)));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnMultipleVersionsOrderedByVersionNumber() throws Exception {
    testQualityCheck.addVersion(
        new QualityCheckVersion(
            testQualityCheck, 1, "SELECT COUNT(*) FROM patients WHERE gender = 'F'"));
    qualityCheckRepository.save(testQualityCheck);
    entityManager.flush();

    testQualityCheck.addVersion(
        new QualityCheckVersion(
            testQualityCheck, 2, "SELECT COUNT(*) FROM patients WHERE gender = 'M'"));
    qualityCheckRepository.save(testQualityCheck);
    entityManager.flush();

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versions").isArray())
        .andExpect(jsonPath("$.versions.length()").value(2))
        .andExpect(jsonPath("$.versions[0].version").value(1))
        .andExpect(jsonPath("$.versions[1].version").value(2));
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
        new QualityCheck("Second Quality Check", "Another test quality check", 0.9, 0.6);
    qualityCheckRepository.save(secondQualityCheck);

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.qualityChecks").isArray())
        .andExpect(jsonPath("$._embedded.qualityChecks.length()").value(2))
        .andExpect(
            jsonPath("$._embedded.qualityChecks[?(@.name == 'Test Quality Check')]").exists())
        .andExpect(
            jsonPath("$._embedded.qualityChecks[?(@.name == 'Second Quality Check')]").exists())
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
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Quality Check"))
        .andExpect(jsonPath("$.description").value("Updated description for the quality check"))
        .andExpect(jsonPath("$.warningThreshold").value(0.75))
        .andExpect(jsonPath("$.errorThreshold").value(0.45))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheck.getId()))
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
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO("Updated Quality Check", "Updated description", 0.75, 0.45, null);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, nonExistentId)
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
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findById_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
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
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId())
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
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
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
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
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
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId())
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
            put(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
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
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category.id").value(testCategory.getId()));

    Long categoryId = testCategory.getId();
    entityManager.clear();
    categoryRepository.deleteById(categoryId);
    entityManager.flush();
    entityManager.clear();

    assertFalse(categoryRepository.existsById(categoryId));

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldReturnKeywordsWhenAdded() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords").isArray())
        .andExpect(jsonPath("$.keywords.length()").value(3))
        .andExpect(jsonPath("$.keywords", hasItems("gender", "sex", "male")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findAll_shouldReturnKeywordsForAllQualityChecks() throws Exception {
    QualityCheck secondQualityCheck =
        new QualityCheck("Second Quality Check", "Another test quality check", 0.9, 0.6);
    secondQualityCheck.setKeywords(Set.of("diagnosis", "C50"));
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
  void setKeywords_shouldReplaceAllKeywordsAndReturnHateoasResponse() throws Exception {
    Set<String> newKeywords = Set.of("patient data", "diagnosis", "treatment");
    KeywordsDTO keywordsDTO = new KeywordsDTO(newKeywords);
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            put(keywordsEndpoint, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords").isArray())
        .andExpect(jsonPath("$.keywords.length()").value(3))
        .andExpect(jsonPath("$.keywords", hasItems("patient data", "diagnosis", "treatment")))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheck.getId()))
        .andExpect(
            jsonPath("$._links.quality-checks.href")
                .value("http://localhost/api/v1/quality-checks"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void setKeywords_shouldClearAllKeywordsWhenEmptySetProvided() throws Exception {
    KeywordsDTO keywordsDTO = new KeywordsDTO(Set.of());
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            put(keywordsEndpoint, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords").isArray())
        .andExpect(jsonPath("$.keywords.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void setKeywords_shouldReturnForbiddenForNonAdminUser() throws Exception {
    KeywordsDTO keywordsDTO = new KeywordsDTO(Set.of("test keyword"));
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            put(keywordsEndpoint, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void setKeywords_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;
    KeywordsDTO keywordsDTO = new KeywordsDTO(Set.of("test keyword"));
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            put(keywordsEndpoint, nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void setKeywords_shouldReturnBadRequestForInvalidData() throws Exception {
    KeywordsDTO keywordsDTO =
        new KeywordsDTO(null); // null keywords should trigger validation error
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            put(keywordsEndpoint, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void setKeywords_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    KeywordsDTO keywordsDTO = new KeywordsDTO(Set.of("test keyword"));
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            put(keywordsEndpoint, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldCreateVersionWithHashAndAutoIncrementVersion() throws Exception {
    String query = "SELECT COUNT(*) FROM patients WHERE gender = 'F'";
    QualityCheckVersionCreateDTO createDTO = new QualityCheckVersionCreateDTO(query, null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.query").value(query))
        .andExpect(jsonPath("$.hash").value(hashOf(query)))
        .andExpect(
            jsonPath("$._links.quality-check-versions.href")
                .value(
                    "http://localhost/api/v1/quality-checks/"
                        + testQualityCheck.getId()
                        + "/versions"))
        .andExpect(
            jsonPath("$._links.quality-check.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheck.getId()));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldIncrementVersionWhenMultipleVersionsCreated() throws Exception {
    String firstQuery = "SELECT COUNT(*) FROM patients WHERE gender = 'F'";
    String secondQuery = "SELECT COUNT(*) FROM patients WHERE gender = 'M'";
    QualityCheckVersionCreateDTO firstDTO = new QualityCheckVersionCreateDTO(firstQuery, null);
    QualityCheckVersionCreateDTO secondDTO = new QualityCheckVersionCreateDTO(secondQuery, null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.hash").value(hashOf(firstQuery)));

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.hash").value(hashOf(secondQuery)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldAcceptExplicitVersionNumber() throws Exception {
    String query = "SELECT COUNT(*) FROM patients WHERE gender = 'F'";
    QualityCheckVersionCreateDTO createDTO = new QualityCheckVersionCreateDTO(query, 7);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(7))
        .andExpect(jsonPath("$.hash").value(hashOf(query)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    QualityCheckVersionCreateDTO createDTO = new QualityCheckVersionCreateDTO("SELECT 1", null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldReturnBadRequestForBlankQuery() throws Exception {
    QualityCheckVersionCreateDTO createDTO = new QualityCheckVersionCreateDTO(" ", null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void createVersion_shouldReturnForbiddenForNonAdminUser() throws Exception {
    QualityCheckVersionCreateDTO createDTO = new QualityCheckVersionCreateDTO("SELECT 1", null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createVersion_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    QualityCheckVersionCreateDTO createDTO = new QualityCheckVersionCreateDTO("SELECT 1", null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findVersions_shouldReturnAllVersionsOrderedByVersionNumber() throws Exception {
    QualityCheckVersionCreateDTO firstDTO =
        new QualityCheckVersionCreateDTO("SELECT COUNT(*) FROM patients WHERE gender = 'F'", null);
    QualityCheckVersionCreateDTO secondDTO =
        new QualityCheckVersionCreateDTO("SELECT COUNT(*) FROM patients WHERE gender = 'M'", null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstDTO)))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondDTO)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.qualityCheckVersions.length()").value(2))
        .andExpect(jsonPath("$._embedded.qualityCheckVersions[0].version").value(1))
        .andExpect(jsonPath("$._embedded.qualityCheckVersions[1].version").value(2))
        .andExpect(
            jsonPath("$._links.self.href")
                .value(
                    "http://localhost/api/v1/quality-checks/"
                        + testQualityCheck.getId()
                        + "/versions"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findVersions_shouldReturnEmptyCollectionWhenNoVersionsExist() throws Exception {
    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_VERSIONS, testQualityCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._links.self.href")
                .value(
                    "http://localhost/api/v1/quality-checks/"
                        + testQualityCheck.getId()
                        + "/versions"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findVersions_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    mockMvc.perform(get(API_V1_QUALITY_CHECKS_VERSIONS, 99999L)).andExpect(status().isNotFound());
  }

  private static String hashOf(String query) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(query.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
