package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckType;
import eu.bbmri_eric.quality.server.dataquality.dto.KeywordsDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
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

    testQualityCheck.setKeywords(Set.of("gender", "sex", "male"));
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
  void create_shouldCreateQualityCheckAndReturnHateoasResponse() throws Exception {
    String expectedHash = sha256("SELECT * FROM patients");
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Created Check",
            "Created description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.7,
            0.4,
            null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hash").value(expectedHash))
        .andExpect(jsonPath("$.name").value("Created Check"))
        .andExpect(jsonPath("$.description").value("Created description"))
        .andExpect(jsonPath("$.query").value("SELECT * FROM patients"))
        .andExpect(jsonPath("$.type").value("SQL"))
        .andExpect(jsonPath("$.warningThreshold").value(0.7))
        .andExpect(jsonPath("$.errorThreshold").value(0.4))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + expectedHash))
        .andExpect(
            jsonPath("$._links.quality-checks.href")
                .value("http://localhost/api/v1/quality-checks"));

    assertTrue(qualityCheckRepository.existsById(expectedHash));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldGenerateSameHashForSameQuery() throws Exception {
    QualityCheckCreateDTO firstDTO =
        new QualityCheckCreateDTO(
            "First Check",
            "description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            null);
    QualityCheckCreateDTO secondDTO =
        new QualityCheckCreateDTO(
            "Second Check",
            "description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            null);

    String firstHash =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        post(API_V1_QUALITY_CHECKS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstDTO)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("hash")
            .asText();

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondDTO)))
        .andExpect(status().isConflict());

    assertEquals(firstHash, sha256("SELECT * FROM patients"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_thenFindById_shouldReturnCreatedQualityCheck() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Findable Check",
            "Created description",
            "SELECT count(*) FROM patients",
            QualityCheckType.FHIR,
            0.8,
            0.5,
            null);
    String expectedHash = sha256("SELECT count(*) FROM patients");

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get(API_V1_QUALITY_CHECKS_ID, expectedHash))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(expectedHash))
        .andExpect(jsonPath("$.name").value("Findable Check"))
        .andExpect(jsonPath("$.query").value("SELECT count(*) FROM patients"))
        .andExpect(jsonPath("$.type").value("FHIR"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_thenUpdate_shouldUpdateCreatedQualityCheck() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Initial Check",
            "Initial description",
            "SELECT count(*) FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            null);
    String initialHash = sha256("SELECT count(*) FROM patients");

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated());

    String newQuery = "SELECT count(*) FROM samples";
    String newHash = sha256(newQuery);
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Updated Check",
            "Updated description",
            newQuery,
            QualityCheckType.SQL,
            0.75,
            0.45,
            null);

    mockMvc
        .perform(
            put(API_V1_QUALITY_CHECKS_ID, initialHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(newHash))
        .andExpect(jsonPath("$.name").value("Updated Check"))
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.query").value(newQuery))
        .andExpect(jsonPath("$.type").value("SQL"))
        .andExpect(jsonPath("$.warningThreshold").value(0.75))
        .andExpect(jsonPath("$.errorThreshold").value(0.45));

    assertFalse(qualityCheckRepository.existsById(initialHash));
    assertTrue(qualityCheckRepository.existsById(newHash));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldAssignCategoryToQualityCheck() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Categorized Check",
            "Created description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            testCategory.getId());

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.category.id").value(testCategory.getId()))
        .andExpect(jsonPath("$.category.name").value("Data Completeness"))
        .andExpect(jsonPath("$.category.colorHex").value("#FF5733"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnConflictWhenQueryAlreadyExists() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "First Check",
            "Created description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Bad Category Check",
            "Created description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            99999L);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForInvalidData() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "",
            "Created description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void create_shouldReturnForbiddenForNonAdminUser() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Forbidden Check",
            "Created description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void create_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Unauthorized Check",
            "Created description",
            "SELECT * FROM patients",
            QualityCheckType.SQL,
            0.8,
            0.5,
            null);

    mockMvc
        .perform(
            post(API_V1_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isUnauthorized());
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
            put(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
        .andExpect(jsonPath("$.keywords").isArray())
        .andExpect(jsonPath("$.keywords.length()").value(3))
        .andExpect(jsonPath("$.keywords", hasItems("patient data", "diagnosis", "treatment")))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/quality-checks/" + testQualityCheckHash))
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
            put(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hash").value(testQualityCheckHash))
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
            put(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void setKeywords_shouldReturnNotFoundWhenQualityCheckDoesNotExist() throws Exception {
    String nonExistentHash = "non-existent-hash";
    KeywordsDTO keywordsDTO = new KeywordsDTO(Set.of("test keyword"));
    String keywordsEndpoint = API_V1_QUALITY_CHECKS_ID + "/keywords";

    mockMvc
        .perform(
            put(keywordsEndpoint, nonExistentHash)
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
            put(keywordsEndpoint, testQualityCheckHash)
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
            put(keywordsEndpoint, testQualityCheckHash)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keywordsDTO)))
        .andExpect(status().isUnauthorized());
  }

  private static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }
}
