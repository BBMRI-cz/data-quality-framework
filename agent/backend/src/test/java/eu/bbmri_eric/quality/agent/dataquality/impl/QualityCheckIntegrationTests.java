package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin")
class QualityCheckIntegrationTests {

  private static final String API_QUALITY_CHECKS = "/api/quality-checks";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private QualityCheckRepository qualityCheckRepository;

  @BeforeEach
  void setUp() {
    qualityCheckRepository.deleteAll();
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
            1.0f);

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
        new QualityCheckCreateDTO("", "Description", "define Test: true", 10, 30, 1.0f);

    mockMvc
        .perform(
            post(API_QUALITY_CHECKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
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
            "Updated Name", "Updated Description", "define Test: false", 20, 50, 2.0f);

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
            "Updated Name", "Updated Description", "define Test: false", 20, 50, 2.0f);

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
            "Updated Name", "Updated Description", "define Test: false", 150, -10, 2.0f);

    mockMvc
        .perform(
            put(API_QUALITY_CHECKS + "/{id}", savedCheck.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
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
    builtInCheck.setEpsilonBudget(0.2f);
    QualityCheck savedCheck = qualityCheckRepository.save(builtInCheck);
    Long checkId = savedCheck.getId();

    QualityCheckUpdateDTO updateDTO =
        new QualityCheckUpdateDTO(
            "Invalid ICD-10 Codes - Updated",
            "Modified description for ICD validation check",
            null, // Keep query as null for built-in check
            15,
            40,
            0.5f);

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
    assertThat(updatedCheck.getEpsilonBudget()).isEqualTo(0.5f);
  }
}
