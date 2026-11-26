package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.domain.Category;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckBulkUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin")
class QualityCheckIntegrationTests {

  private static final String API_QUALITY_CHECKS = "/api/quality-checks";
  private static final String API_CATEGORIES = "/api/categories";

  @Autowired private MockMvc mockMvc;

  @Autowired private JsonMapper objectMapper;

  @Autowired private QualityCheckRepository qualityCheckRepository;

  @Autowired private CategoryRepository categoryRepository;

  @BeforeEach
  void setUp() {
    qualityCheckRepository.deleteAll();
    categoryRepository.deleteAll();
  }

  @Test
  void create_validQualityCheck_returnsCreatedWithLocation() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Patient Age Validation",
            "Validates patient ages are within acceptable range",
            "define Test: true",
            10,
            30,
            1.0);

    String location =
        mockMvc
            .perform(
                post(API_QUALITY_CHECKS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Patient Age Validation"))
            .andExpect(
                jsonPath("$.description")
                    .value("Validates patient ages are within acceptable range"))
            .andExpect(jsonPath("$.query").value("define Test: true"))
            .andExpect(jsonPath("$.warningThreshold").value(10))
            .andExpect(jsonPath("$.errorThreshold").value(30))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();
    assertThat(qualityCheckRepository.count()).isEqualTo(1);
  }

  @Test
  void create_invalidQualityCheck_missingRequiredFields_returnsBadRequest() throws Exception {
    String invalidJson = "{\"name\": \"Invalid Query\"}";

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS).contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void create_invalidQualityCheck_blankName_returnsBadRequest() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO("", "Description", "define Test: true", 10, 30, 1.0);

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void create_qualityCheckWithNonPositiveEpsilonBudget_returnsBadRequest() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO("Test Check", "Description", "define Test: true", 10, 30, 0.0);

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());

    createDTO.setEpsilonBudget(-0.5);
    mockMvc
        .perform(
            post(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());

    assertThat(qualityCheckRepository.count()).isEqualTo(0);
  }

  @Test
  void update_qualityCheckWithEpsilonBudget_canClearPreference() throws Exception {
    QualityCheck check = new QualityCheck("Test Check", "Test Description", "define Test: true");
    check.setEpsilonBudget(0.5);
    check = qualityCheckRepository.save(check);

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Test Check",
            "Test Description",
            "define Test: true",
            QualityCheckType.CQL,
            10,
            30,
            null);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", check.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk());

    assertThat(qualityCheckRepository.findById(check.getId()).orElseThrow().getEpsilonBudget())
        .isNull();
  }

  @Test
  void update_qualityCheckWithoutEpsilonBudget_keepsPreferenceUnset() throws Exception {
    QualityCheck check = qualityCheckRepository.save(new QualityCheck("Test Check", "d", "q"));

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO("Test Check", "d", "q", QualityCheckType.CQL, 10, 30, null);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", check.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk());

    assertThat(qualityCheckRepository.findById(check.getId()).orElseThrow().getEpsilonBudget())
        .isNull();
  }

  @Test
  void findById_existingQualityCheck_returnsOk() throws Exception {
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Test Check", "Test Description", "define Test: true"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS + "/{id}", savedCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(savedCheck.getId()))
        .andExpect(jsonPath("$.name").value("Test Check"))
        .andExpect(jsonPath("$.description").value("Test Description"))
        .andExpect(jsonPath("$.query").value("define Test: true"));
  }

  @Test
  void findById_nonExistingQualityCheck_returnsNotFound() throws Exception {
    mockMvc.perform(get(API_QUALITY_CHECKS + "/{id}", 99999L)).andExpect(status().isNotFound());
  }

  @Test
  void findById_malformedId_returnsBadRequest() throws Exception {
    mockMvc.perform(get(API_QUALITY_CHECKS + "/abc")).andExpect(status().isBadRequest());
  }

  @Test
  void findAll_returnsEmbeddedList() throws Exception {
    qualityCheckRepository.save(new QualityCheck("Check 1", "Description 1", "define Test: true"));
    qualityCheckRepository.save(new QualityCheck("Check 2", "Description 2", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.quality-checks").exists())
        .andExpect(jsonPath("$._embedded.quality-checks.length()").value(2));
  }

  @Test
  void findAll_withPagination_returnsPagedResults() throws Exception {
    for (int i = 0; i < 15; i++) {
      qualityCheckRepository.save(
          new QualityCheck("Check " + i, "Description " + i, "define Test: " + i));
    }

    mockMvc
        .perform(get(API_QUALITY_CHECKS).param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.quality-checks").isArray())
        .andExpect(jsonPath("$._embedded.quality-checks.length()").value(10))
        .andExpect(jsonPath("$.page.size").value(10))
        .andExpect(jsonPath("$.page.totalElements").value(15))
        .andExpect(jsonPath("$.page.totalPages").value(2))
        .andExpect(jsonPath("$._links.next").exists());

    mockMvc
        .perform(get(API_QUALITY_CHECKS).param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.quality-checks").isArray())
        .andExpect(jsonPath("$._embedded.quality-checks.length()").value(5));
  }

  @Test
  void update_existingQualityCheck_returnsOkWithUpdatedData() throws Exception {
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Original Name", "Original Description", "define Test: true"));

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Updated Name",
            "Updated Description",
            "define Test: false",
            QualityCheckType.SQL,
            20,
            50,
            2.0);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", savedCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.description").value("Updated Description"))
        .andExpect(jsonPath("$.query").value("define Test: false"))
        .andExpect(jsonPath("$.warningThreshold").value(20))
        .andExpect(jsonPath("$.errorThreshold").value(50));

    QualityCheck updatedCheck = qualityCheckRepository.findById(savedCheck.getId()).orElseThrow();
    assertThat(updatedCheck.getName()).isEqualTo("Updated Name");
    assertThat(updatedCheck.getDescription()).isEqualTo("Updated Description");
  }

  @Test
  void update_nonExistingQualityCheck_returnsNotFound() throws Exception {
    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Updated Name",
            "Updated Description",
            "define Test: false",
            QualityCheckType.SQL,
            20,
            50,
            2.0);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  void update_invalidThresholds_returnsBadRequest() throws Exception {
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Test Check", "Test Description", "define Test: true"));

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Updated Name",
            "Updated Description",
            "define Test: false",
            QualityCheckType.SQL,
            150,
            -10,
            2.0);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", savedCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_existingQualityCheck_togglesActive() throws Exception {
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Active Test", "Initially active", "define Test: true"));
    assertThat(savedCheck.isActive()).isTrue();

    QualityCheckUpdateDTO updateDTO = new QualityCheckUpdateDTO();
    updateDTO.setActive(false);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", savedCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    assertThat(qualityCheckRepository.findById(savedCheck.getId()).orElseThrow().isActive())
        .isFalse();
  }

  @Test
  void updateAll_existingChecks_updatesActiveFlagsAndPreservesOtherFields() throws Exception {
    QualityCheck first =
        qualityCheckRepository.save(
            new QualityCheck("First Check", "First Description", "define First: true"));
    QualityCheck second =
        qualityCheckRepository.save(
            new QualityCheck("Second Check", "Second Description", "define Second: true"));

    QualityCheckBulkUpdateDTO firstUpdate = new QualityCheckBulkUpdateDTO();
    firstUpdate.setId(first.getId());
    firstUpdate.setActive(false);
    QualityCheckBulkUpdateDTO secondUpdate = new QualityCheckBulkUpdateDTO();
    secondUpdate.setId(second.getId());
    secondUpdate.setActive(false);

    mockMvc
        .perform(
            patch(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(firstUpdate, secondUpdate))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].active").value(false))
        .andExpect(jsonPath("$[1].active").value(false));

    QualityCheck updatedFirst = qualityCheckRepository.findById(first.getId()).orElseThrow();
    QualityCheck updatedSecond = qualityCheckRepository.findById(second.getId()).orElseThrow();
    assertThat(updatedFirst.isActive()).isFalse();
    assertThat(updatedSecond.isActive()).isFalse();
    assertThat(updatedFirst.getName()).isEqualTo("First Check");
    assertThat(updatedFirst.getDescription()).isEqualTo("First Description");
    assertThat(updatedSecond.getName()).isEqualTo("Second Check");
    assertThat(updatedSecond.getDescription()).isEqualTo("Second Description");
  }

  @Test
  void updateAll_nonExistingCheck_returnsNotFoundAndAppliesNoChanges() throws Exception {
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Existing Check", "Should stay active", "define Test: true"));

    QualityCheckBulkUpdateDTO existingUpdate = new QualityCheckBulkUpdateDTO();
    existingUpdate.setId(savedCheck.getId());
    existingUpdate.setActive(false);
    QualityCheckBulkUpdateDTO missingUpdate = new QualityCheckBulkUpdateDTO();
    missingUpdate.setId(99999L);
    missingUpdate.setActive(false);

    mockMvc
        .perform(
            patch(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(existingUpdate, missingUpdate))))
        .andExpect(status().isNotFound());

    assertThat(qualityCheckRepository.findById(savedCheck.getId()).orElseThrow().isActive())
        .isTrue();
  }

  @Test
  void updateAll_missingId_returnsBadRequest() throws Exception {
    QualityCheckBulkUpdateDTO updateDTO = new QualityCheckBulkUpdateDTO();
    updateDTO.setActive(false);

    mockMvc
        .perform(
            patch(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(updateDTO))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateAll_emptyList_returnsBadRequest() throws Exception {
    mockMvc
        .perform(patch(API_QUALITY_CHECKS).contentType(MediaType.APPLICATION_JSON).content("[]"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateAll_checkWithCategory_preservesCategory() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Categorized Check", "Has a category", "define Test: true"));
    savedCheck.setCategory(category);
    savedCheck = qualityCheckRepository.save(savedCheck);

    QualityCheckBulkUpdateDTO updateDTO = new QualityCheckBulkUpdateDTO();
    updateDTO.setId(savedCheck.getId());
    updateDTO.setActive(false);

    mockMvc
        .perform(
            patch(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(updateDTO))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].active").value(false))
        .andExpect(jsonPath("$[0].category.id").value(category.getId()));

    QualityCheck updated = qualityCheckRepository.findById(savedCheck.getId()).orElseThrow();
    assertThat(updated.isActive()).isFalse();
    assertThat(updated.getCategory()).isNotNull();
    assertThat(updated.getCategory().getId()).isEqualTo(category.getId());
  }

  @Test
  void repository_findAllByActive_returnsOnlyActiveChecks() {
    QualityCheck activeCheck =
        qualityCheckRepository.save(
            new QualityCheck("Active Query Check", "Is active", "define Test: true"));
    QualityCheck inactiveCheck =
        new QualityCheck("Inactive Query Check", "Is inactive", "define Test: true");
    inactiveCheck.setActive(false);
    inactiveCheck = qualityCheckRepository.save(inactiveCheck);

    List<Long> activeIds =
        Arrays.stream(qualityCheckRepository.findAllByActive(true))
            .map(QualityCheck::getId)
            .toList();

    assertThat(activeIds).contains(activeCheck.getId()).doesNotContain(inactiveCheck.getId());
  }

  @Test
  void delete_existingQualityCheck_returnsNoContent() throws Exception {
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Delete Test", "To be deleted", "define Test: true"));

    mockMvc
        .perform(delete(API_QUALITY_CHECKS + "/{id}", savedCheck.getId()))
        .andExpect(status().isNoContent());

    assertThat(qualityCheckRepository.existsById(savedCheck.getId())).isFalse();
  }

  @Test
  void delete_nonExistingQualityCheck_returnsNotFound() throws Exception {
    mockMvc.perform(delete(API_QUALITY_CHECKS + "/{id}", 99999L)).andExpect(status().isNotFound());
  }

  @Test
  void delete_thenGet_returnsNotFound() throws Exception {
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Delete Test", "To be deleted", "define Test: true"));

    mockMvc
        .perform(delete(API_QUALITY_CHECKS + "/{id}", savedCheck.getId()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(API_QUALITY_CHECKS + "/{id}", savedCheck.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void update_builtInCheck_modifiesThresholdsAndDescription() throws Exception {
    // Create a built-in check (simulating a custom check with null query)
    QualityCheck builtInCheck =
        new QualityCheck(
            "Invalid ICD-10 Codes", "How many conditions have invalid ICD-10 codes", null);
    builtInCheck.setWarningThreshold(10);
    builtInCheck.setErrorThreshold(30);
    builtInCheck.setEpsilonBudget(0.2);
    QualityCheck savedCheck = qualityCheckRepository.save(builtInCheck);
    Long checkId = savedCheck.getId();

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Invalid ICD-10 Codes - Updated",
            "Modified description for ICD validation check",
            null, // Keep query as null for built-in check
            QualityCheckType.JAVA,
            15,
            40,
            0.5);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", checkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(checkId))
        .andExpect(jsonPath("$.name").value("Invalid ICD-10 Codes - Updated"))
        .andExpect(jsonPath("$.description").value("Modified description for ICD validation check"))
        .andExpect(jsonPath("$.warningThreshold").value(15))
        .andExpect(jsonPath("$.errorThreshold").value(40))
        .andExpect(jsonPath("$.epsilonBudget").value(0.5));

    QualityCheck updatedCheck = qualityCheckRepository.findById(checkId).orElseThrow();
    assertThat(updatedCheck.getName()).isEqualTo("Invalid ICD-10 Codes - Updated");
    assertThat(updatedCheck.getWarningThreshold()).isEqualTo(15);
    assertThat(updatedCheck.getErrorThreshold()).isEqualTo(40);
    assertThat(updatedCheck.getEpsilonBudget()).isEqualTo(0.5);
  }

  @Test
  void create_qualityCheckWithCategory_returnsCreatedWithCategory() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));

    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Categorized Check",
            "Check with category",
            "define Test: true",
            QualityCheckType.CQL,
            10,
            30,
            1.0,
            category.getId());

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Categorized Check"))
        .andExpect(jsonPath("$.category.id").value(category.getId()))
        .andExpect(jsonPath("$.category.name").value("Data Completeness"))
        .andExpect(jsonPath("$.category.colorHex").value("#FF5733"));
  }

  @Test
  void update_qualityCheckWithCategory_assignsCategory() throws Exception {
    Category category = categoryRepository.save(new Category("Data Accuracy", "#33A1FF"));
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Check Without Category", "No category", "define Test: true"));

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Check Without Category",
            "No category",
            "define Test: true",
            QualityCheckType.CQL,
            10,
            30,
            1.0,
            category.getId());

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", savedCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category.id").value(category.getId()))
        .andExpect(jsonPath("$.category.name").value("Data Accuracy"));

    QualityCheck updatedCheck = qualityCheckRepository.findById(savedCheck.getId()).orElseThrow();
    assertThat(updatedCheck.getCategory()).isNotNull();
    assertThat(updatedCheck.getCategory().getName()).isEqualTo("Data Accuracy");
  }

  @Test
  void update_qualityCheckWithCategory_removesCategoryWhenNull() throws Exception {
    Category category = categoryRepository.save(new Category("Data Accuracy", "#33A1FF"));
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Check With Category", "Has category", "define Test: true"));
    savedCheck.setCategory(category);
    qualityCheckRepository.save(savedCheck);

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Check With Category",
            "Has category",
            "define Test: true",
            QualityCheckType.CQL,
            10,
            30,
            1.0,
            null);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", savedCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category").doesNotExist());

    QualityCheck updatedCheck = qualityCheckRepository.findById(savedCheck.getId()).orElseThrow();
    assertThat(updatedCheck.getCategory()).isNull();
  }

  @Test
  void create_qualityCheckWithNonExistingCategory_returnsNotFound() throws Exception {
    QualityCheckCreateDTO createDTO =
        new QualityCheckCreateDTO(
            "Categorized Check",
            "Check with missing category",
            "define Test: true",
            QualityCheckType.CQL,
            10,
            30,
            1.0,
            99999L);

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_categoryAssignedToQualityCheck_qualityCheckExistsWithoutCategory() throws Exception {
    Category category = categoryRepository.save(new Category("Data Accuracy", "#33A1FF"));
    QualityCheck savedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Check With Category", "Has category", "define Test: true"));
    savedCheck.setCategory(category);
    qualityCheckRepository.save(savedCheck);

    mockMvc
        .perform(delete(API_CATEGORIES + "/{id}", category.getId()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(API_QUALITY_CHECKS + "/{id}", savedCheck.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(savedCheck.getId()))
        .andExpect(jsonPath("$.name").value("Check With Category"))
        .andExpect(jsonPath("$.category").doesNotExist());

    QualityCheck updatedCheck = qualityCheckRepository.findById(savedCheck.getId()).orElseThrow();
    assertThat(updatedCheck.getCategory()).isNull();
  }

  @Test
  void findAll_withCategoryNameFilter_returnsMatchingQualityChecks() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));
    QualityCheck categorizedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Categorized Check", "With category", "define Test: true"));
    categorizedCheck.setCategory(category);
    qualityCheckRepository.save(categorizedCheck);
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS).param("categoryName", "Data Completeness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.quality-checks").isArray())
        .andExpect(jsonPath("$._embedded.quality-checks.length()").value(1))
        .andExpect(jsonPath("$._embedded.quality-checks[0].name").value("Categorized Check"))
        .andExpect(
            jsonPath("$._embedded.quality-checks[0].category.name").value("Data Completeness"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void findAll_withNonExistingCategoryNameFilter_returnsEmptyPage() throws Exception {
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: true"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS).param("categoryName", "Non Existing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$.page.totalElements").value(0));
  }

  @Test
  void findAll_withoutCategoryNameFilter_returnsAllQualityChecks() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));
    QualityCheck categorizedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Categorized Check", "With category", "define Test: true"));
    categorizedCheck.setCategory(category);
    qualityCheckRepository.save(categorizedCheck);
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.quality-checks").isArray())
        .andExpect(jsonPath("$._embedded.quality-checks.length()").value(2))
        .andExpect(jsonPath("$.page.totalElements").value(2));
  }

  @Test
  void export_all_returnsJsonAttachmentWithPortableFields() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));
    QualityCheck categorizedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Categorized Check", "With category", "define Test: true"));
    categorizedCheck.setType(QualityCheckType.SQL);
    categorizedCheck.setWarningThreshold(15);
    categorizedCheck.setErrorThreshold(45);
    categorizedCheck.setEpsilonBudget(1.0);
    categorizedCheck.setCategory(category);
    qualityCheckRepository.save(categorizedCheck);
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS + "/export"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    containsString("attachment; filename=\"quality-checks.json\"")))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Categorized Check"))
        .andExpect(jsonPath("$[0].description").value("With category"))
        .andExpect(jsonPath("$[0].query").value("define Test: true"))
        .andExpect(jsonPath("$[0].type").value("SQL"))
        .andExpect(jsonPath("$[0].warningThreshold").value(15))
        .andExpect(jsonPath("$[0].errorThreshold").value(45))
        .andExpect(jsonPath("$[0].epsilonBudget").value(1.0))
        .andExpect(jsonPath("$[0].category").value("Data Completeness"))
        .andExpect(jsonPath("$[1].name").value("Uncategorized Check"))
        .andExpect(jsonPath("$[1].category").doesNotExist())
        .andExpect(jsonPath("$[1].epsilonBudget").doesNotExist())
        .andExpect(jsonPath("$[*].id").doesNotExist())
        .andExpect(jsonPath("$[*].active").doesNotExist());
  }

  @Test
  void export_withCategoryNameFilter_returnsOnlyMatchingChecks() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));
    QualityCheck categorizedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Categorized Check", "With category", "define Test: true"));
    categorizedCheck.setCategory(category);
    qualityCheckRepository.save(categorizedCheck);
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS + "/export").param("categoryName", "Data Completeness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Categorized Check"));
  }

  @Test
  void export_withBlankCategoryName_returnsOnlyUncategorized() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));
    QualityCheck categorizedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Categorized Check", "With category", "define Test: true"));
    categorizedCheck.setCategory(category);
    qualityCheckRepository.save(categorizedCheck);
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS + "/export").param("categoryName", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Uncategorized Check"));
  }

  @Test
  @WithAnonymousUser
  void export_withoutAuthentication_returnsUnauthorized() throws Exception {
    mockMvc.perform(get(API_QUALITY_CHECKS + "/export")).andExpect(status().isUnauthorized());
  }

  @Test
  void findAll_withMultipleCategoryNames_returnsChecksFromAllCategories() throws Exception {
    saveCheckInCategory("Data Completeness", "Completeness Check");
    saveCheckInCategory("Validity", "Validity Check");
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS).param("categoryName", "Data Completeness", "Validity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.quality-checks.length()").value(2))
        .andExpect(jsonPath("$.page.totalElements").value(2));
  }

  @Test
  void export_withMultipleCategoryNames_returnsChecksFromAllCategories() throws Exception {
    saveCheckInCategory("Data Completeness", "Completeness Check");
    saveCheckInCategory("Validity", "Validity Check");
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(
            get(API_QUALITY_CHECKS + "/export")
                .param("categoryName", "Data Completeness", "Validity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Completeness Check"))
        .andExpect(jsonPath("$[1].name").value("Validity Check"));
  }

  @Test
  void export_withCategoryNameAndBlank_returnsCategoryAndUncategorized() throws Exception {
    saveCheckInCategory("Data Completeness", "Completeness Check");
    saveCheckInCategory("Validity", "Validity Check");
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS + "/export").param("categoryName", "Data Completeness", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Completeness Check"))
        .andExpect(jsonPath("$[1].name").value("Uncategorized Check"));
  }

  private void saveCheckInCategory(String categoryName, String checkName) {
    Category category = categoryRepository.save(new Category(categoryName, "#FF5733"));
    QualityCheck qualityCheck =
        qualityCheckRepository.save(
            new QualityCheck(checkName, "With category", "define Test: true"));
    qualityCheck.setCategory(category);
    qualityCheckRepository.save(qualityCheck);
  }

  @Test
  void findAll_withEmptyCategoryNameFilter_returnsUncategorizedQualityChecks() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));
    QualityCheck categorizedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Categorized Check", "With category", "define Test: true"));
    categorizedCheck.setCategory(category);
    qualityCheckRepository.save(categorizedCheck);
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));

    mockMvc
        .perform(get(API_QUALITY_CHECKS).param("categoryName", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.quality-checks").isArray())
        .andExpect(jsonPath("$._embedded.quality-checks.length()").value(1))
        .andExpect(jsonPath("$._embedded.quality-checks[0].name").value("Uncategorized Check"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void import_newChecks_createsThem() throws Exception {
    String entries =
        """
        [
          {
            "name": "Imported SQL check",
            "description": "Imported via file",
            "query": "SELECT 1",
            "type": "SQL",
            "warningThreshold": 15,
            "errorThreshold": 45,
            "category": "Imported Category"
          },
          {
            "name": "Imported CQL check",
            "description": "With epsilon",
            "query": "define Test: true",
            "type": "CQL",
            "warningThreshold": 10,
            "errorThreshold": 30,
            "epsilonBudget": 1.0
          }
        ]
        """;

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entries))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(2))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.unchanged").value(0));

    List<QualityCheck> checks = qualityCheckRepository.findAll();
    assertThat(checks).hasSize(2);
    QualityCheck sqlCheck = findCheckByName(checks, "Imported SQL check");
    assertThat(sqlCheck.getType()).isEqualTo(QualityCheckType.SQL);
    assertThat(sqlCheck.getQuery()).isEqualTo("SELECT 1");
    assertThat(sqlCheck.getDescription()).isEqualTo("Imported via file");
    assertThat(sqlCheck.getWarningThreshold()).isEqualTo(15);
    assertThat(sqlCheck.getErrorThreshold()).isEqualTo(45);
    assertThat(sqlCheck.getEpsilonBudget()).isNull();
    assertThat(sqlCheck.getCategory()).isNotNull();
    assertThat(sqlCheck.getCategory().getName()).isEqualTo("Imported Category");
    QualityCheck cqlCheck = findCheckByName(checks, "Imported CQL check");
    assertThat(cqlCheck.getType()).isEqualTo(QualityCheckType.CQL);
    assertThat(cqlCheck.getEpsilonBudget()).isEqualTo(1.0);
    assertThat(cqlCheck.getCategory()).isNull();
    assertThat(cqlCheck.isActive()).isTrue();
  }

  @Test
  void import_sameFileTwice_reportsAllUnchanged() throws Exception {
    String entries =
        """
        [
          {
            "name": "Stable check",
            "description": "One",
            "query": "SELECT 1",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          },
          {
            "name": "Other check",
            "description": "Two",
            "query": "define Test: true",
            "type": "CQL",
            "warningThreshold": 20,
            "errorThreshold": 40,
            "epsilonBudget": 0.5
          }
        ]
        """;

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entries))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(2));

    List<Long> idsBefore =
        qualityCheckRepository.findAll().stream().map(QualityCheck::getId).sorted().toList();

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entries))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.unchanged").value(2));

    List<Long> idsAfter =
        qualityCheckRepository.findAll().stream().map(QualityCheck::getId).sorted().toList();
    assertThat(idsAfter).isEqualTo(idsBefore);
  }

  @Test
  void import_changedFields_updatesExistingOnly() throws Exception {
    String firstImport =
        """
        [
          {
            "name": "Check A",
            "description": "First",
            "query": "SELECT 1",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          },
          {
            "name": "Check B",
            "description": "Second",
            "query": "define Test: true",
            "type": "CQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          },
          {
            "name": "Check C",
            "description": "Third",
            "query": "SELECT 3",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          }
        ]
        """;
    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstImport))
        .andExpect(status().isOk());
    QualityCheck checkA = findCheckByName(qualityCheckRepository.findAll(), "Check A");
    checkA.setActive(false);
    qualityCheckRepository.save(checkA);

    String secondImport =
        """
        [
          {
            "name": "Check A",
            "description": "First",
            "query": "SELECT 1",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 50
          },
          {
            "name": "Check B",
            "description": "Second",
            "query": "define Test: false",
            "type": "CQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          },
          {
            "name": "Check C",
            "description": "Third",
            "query": "SELECT 3",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          }
        ]
        """;
    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondImport))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.updated").value(2))
        .andExpect(jsonPath("$.unchanged").value(1));

    List<QualityCheck> checks = qualityCheckRepository.findAll();
    assertThat(checks).hasSize(3);
    QualityCheck updatedA = findCheckByName(checks, "Check A");
    assertThat(updatedA.getErrorThreshold()).isEqualTo(50);
    assertThat(updatedA.getWarningThreshold()).isEqualTo(10);
    assertThat(updatedA.isActive()).isFalse();
    QualityCheck updatedB = findCheckByName(checks, "Check B");
    assertThat(updatedB.getQuery()).isEqualTo("define Test: false");
    QualityCheck unchangedC = findCheckByName(checks, "Check C");
    assertThat(unchangedC.getDescription()).isEqualTo("Third");
    assertThat(unchangedC.isActive()).isTrue();
  }

  @Test
  void import_categoryByName_matchesExistingAndCreatesMissing() throws Exception {
    Category existingCategory = categoryRepository.save(new Category("Alpha", "#FF5733"));
    String entries =
        """
        [
          {
            "name": "Check in Alpha",
            "description": "Existing",
            "query": "SELECT 1",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30,
            "category": "Alpha"
          },
          {
            "name": "Check in Beta",
            "description": "Missing",
            "query": "SELECT 2",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30,
            "category": "Beta"
          }
        ]
        """;

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entries))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(2));

    List<QualityCheck> checks = qualityCheckRepository.findAll();
    QualityCheck alphaCheck = findCheckByName(checks, "Check in Alpha");
    assertThat(alphaCheck.getCategory()).isNotNull();
    assertThat(alphaCheck.getCategory().getId()).isEqualTo(existingCategory.getId());
    assertThat(alphaCheck.getCategory().getColorHex()).isEqualTo("#FF5733");
    QualityCheck betaCheck = findCheckByName(checks, "Check in Beta");
    assertThat(betaCheck.getCategory()).isNotNull();
    assertThat(betaCheck.getCategory().getName()).isEqualTo("Beta");
    assertThat(betaCheck.getCategory().getColorHex()).isNull();
  }

  @Test
  void import_invalidEntry_returns400AndSavesNothing() throws Exception {
    String entries =
        """
        [
          {
            "name": "Valid check",
            "description": "Ok",
            "query": "SELECT 1",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          },
          {
            "name": "Threshold-less check",
            "description": "Missing warning threshold",
            "query": "SELECT 2",
            "type": "SQL",
            "errorThreshold": 30
          }
        ]
        """;

    String response =
        mockMvc
            .perform(
                post(API_QUALITY_CHECKS + "/import")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(entries))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).contains("warningThreshold").contains("[1]");
    assertThat(qualityCheckRepository.count()).isZero();
    assertThat(categoryRepository.count()).isZero();
  }

  @Test
  void import_duplicateNamesInFile_returns400AndSavesNothing() throws Exception {
    String entries =
        """
        [
          {
            "name": "Twice named",
            "description": "First",
            "query": "SELECT 1",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          },
          {
            "name": "Twice named",
            "description": "Second",
            "query": "SELECT 2",
            "type": "SQL",
            "warningThreshold": 10,
            "errorThreshold": 30
          }
        ]
        """;

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entries))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Import Validation Failed"))
        .andExpect(jsonPath("$.invalidEntries.length()").value(2))
        .andExpect(jsonPath("$.invalidEntries[0].index").value(1))
        .andExpect(jsonPath("$.invalidEntries[1].index").value(2))
        .andExpect(jsonPath("$.invalidEntries[0].name").value("Twice named"))
        .andExpect(
            jsonPath("$.invalidEntries[0].reason")
                .value("the name appears more than once in the file"));

    assertThat(qualityCheckRepository.count()).isZero();
  }

  @Test
  void import_nameMatchingMultipleExistingChecks_returns400() throws Exception {
    qualityCheckRepository.save(new QualityCheck("Ambiguous", "First one", "SELECT 1"));
    qualityCheckRepository.save(new QualityCheck("Ambiguous", "Second one", "SELECT 2"));

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    [
                      {
                        "name": "Ambiguous",
                        "description": "Updated",
                        "query": "SELECT 3",
                        "type": "SQL",
                        "warningThreshold": 10,
                        "errorThreshold": 30
                      }
                    ]
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.invalidEntries[0].name").value("Ambiguous"))
        .andExpect(
            jsonPath("$.invalidEntries[0].reason")
                .value("multiple existing checks are named 'Ambiguous'"));

    assertThat(qualityCheckRepository.count()).isEqualTo(2);
  }

  @Test
  void import_javaEntry_updatesExistingBuiltIn() throws Exception {
    QualityCheck builtin = new QualityCheck("Duplicate identifiers", "Duplicate patients", null);
    builtin.setType(QualityCheckType.JAVA);
    qualityCheckRepository.save(builtin);

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    [
                      {
                        "name": "Duplicate identifiers",
                        "description": "Duplicate patients",
                        "type": "JAVA",
                        "warningThreshold": 20,
                        "errorThreshold": 40
                      }
                    ]
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.updated").value(1))
        .andExpect(jsonPath("$.unchanged").value(0));

    QualityCheck updated =
        findCheckByName(qualityCheckRepository.findAll(), "Duplicate identifiers");
    assertThat(updated.getQuery()).isNull();
    assertThat(updated.getWarningThreshold()).isEqualTo(20);
    assertThat(updated.getErrorThreshold()).isEqualTo(40);
  }

  @Test
  void import_javaEntryWithoutMatchingCheck_returns400() throws Exception {
    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    [
                      {
                        "name": "Unknown built-in",
                        "description": "Cannot be created",
                        "type": "JAVA",
                        "warningThreshold": 10,
                        "errorThreshold": 30
                      }
                    ]
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Import Validation Failed"))
        .andExpect(jsonPath("$.invalidEntries[0].index").value(1))
        .andExpect(jsonPath("$.invalidEntries[0].name").value("Unknown built-in"))
        .andExpect(
            jsonPath("$.invalidEntries[0].reason")
                .value(
                    "no existing built-in check named 'Unknown built-in';"
                        + " JAVA checks can only be updated, never created"));

    assertThat(qualityCheckRepository.count()).isZero();
  }

  @Test
  void import_nonJavaEntryMatchingBuiltIn_returns400AndKeepsBuiltIn() throws Exception {
    QualityCheck builtin = new QualityCheck("Duplicate identifiers", "Duplicate patients", null);
    builtin.setType(QualityCheckType.JAVA);
    qualityCheckRepository.save(builtin);

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    [
                      {
                        "name": "Duplicate identifiers",
                        "query": "SELECT 1",
                        "type": "SQL",
                        "warningThreshold": 10,
                        "errorThreshold": 30
                      }
                    ]
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.invalidEntries[0].index").value(1))
        .andExpect(
            jsonPath("$.invalidEntries[0].reason")
                .value(
                    "'Duplicate identifiers' is a built-in check; its type cannot be changed to SQL"));

    QualityCheck reloaded = qualityCheckRepository.findById(builtin.getId()).orElseThrow();
    assertThat(reloaded.getType()).isEqualTo(QualityCheckType.JAVA);
    assertThat(reloaded.getQuery()).isNull();
  }

  @Test
  void import_javaEntryWithQuery_ignoresQuery() throws Exception {
    QualityCheck builtin = new QualityCheck("Builtin check", "Built-in logic", "BUILT-IN");
    builtin.setType(QualityCheckType.JAVA);
    qualityCheckRepository.save(builtin);

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    [
                      {
                        "name": "Builtin check",
                        "description": "Built-in logic",
                        "query": "SHOULD BE IGNORED",
                        "type": "JAVA",
                        "warningThreshold": 25,
                        "errorThreshold": 30
                      }
                    ]
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(1));

    QualityCheck updated = findCheckByName(qualityCheckRepository.findAll(), "Builtin check");
    assertThat(updated.getQuery()).isEqualTo("BUILT-IN");
    assertThat(updated.getWarningThreshold()).isEqualTo(25);
  }

  @Test
  @WithAnonymousUser
  void import_withoutAuthentication_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void import_emptyArray_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void import_malformedJson_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not a json array"))
        .andExpect(status().isBadRequest());

    assertThat(qualityCheckRepository.count()).isZero();
  }

  @Test
  void exportImportRoundTrip_recreatesChecksAndReImportIsUnchanged() throws Exception {
    Category category = categoryRepository.save(new Category("Data Completeness", "#FF5733"));
    QualityCheck categorizedCheck =
        qualityCheckRepository.save(
            new QualityCheck("Categorized Check", "With category", "define Test: true"));
    categorizedCheck.setType(QualityCheckType.SQL);
    categorizedCheck.setEpsilonBudget(1.0);
    categorizedCheck.setCategory(category);
    qualityCheckRepository.save(categorizedCheck);
    qualityCheckRepository.save(
        new QualityCheck("Uncategorized Check", "Without category", "define Test: false"));
    QualityCheck builtin = new QualityCheck("Duplicate identifiers", "Duplicate patients", null);
    builtin.setType(QualityCheckType.JAVA);
    builtin.setEpsilonBudget(0.2);
    qualityCheckRepository.save(builtin);

    String exported =
        mockMvc
            .perform(get(API_QUALITY_CHECKS + "/export"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    qualityCheckRepository.delete(categorizedCheck);
    qualityCheckRepository.delete(
        findCheckByName(qualityCheckRepository.findAll(), "Uncategorized Check"));

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exported))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(2))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.unchanged").value(1));

    String exportedAgain =
        mockMvc
            .perform(get(API_QUALITY_CHECKS + "/export"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(exportedAgain).isEqualTo(exported);

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS + "/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exported))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.unchanged").value(3));
  }

  private static QualityCheck findCheckByName(List<QualityCheck> checks, String name) {
    return checks.stream().filter(check -> check.getName().equals(name)).findFirst().orElseThrow();
  }
}
