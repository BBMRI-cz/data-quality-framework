package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ReportCreateRequest;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerTest {

  public static final String API_V1_AGENTS_REPORTS = "/api/v1/agents/{agentId}/reports";
  public static final String API_V1_REPORTS_ID = "/api/v1/reports/{id}";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ReportRepository reportRepository;
  @Autowired private AgentRepository agentRepository;
  @Autowired private QualityCheckRepository qualityCheckRepository;

  private String testAgentId;
  private Agent testAgent;

  @BeforeEach
  void setUp() {
    agentRepository.deleteAll();
    testAgentId = UUID.randomUUID().toString();
    Agent agent = new Agent(testAgentId);
    testAgent = agentRepository.save(agent);
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldCreateReportAndReturnHateoasResponse() throws Exception {
    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO("hash1", 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.agentId").value(testAgentId))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$._links.self.href").exists())
        .andExpect(jsonPath("$._links.reports.href").value("http://localhost/api/v1"));
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldCreateReportWithResults() throws Exception {
    List<QualityCheckResultDTO> results =
        List.of(new QualityCheckResultDTO("hash1", 0.95), new QualityCheckResultDTO("hash2", 0.87));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.agentId").value(testAgentId))
        .andExpect(jsonPath("$.id").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForNullResults() throws Exception {
    assertThrows(NullPointerException.class, () -> new ReportCreateRequest(null));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForEmptyResults() throws Exception {
    ReportCreateRequest createRequest = new ReportCreateRequest(Collections.emptyList());

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnReportWithHateoasLinksWhenExists() throws Exception {
    Report report = new Report();
    testAgent.addReport(report);
    agentRepository.saveAndFlush(testAgent);
    mockMvc
        .perform(get(API_V1_REPORTS_ID, report.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(report.getId()))
        .andExpect(jsonPath("$.agentId").value(testAgentId))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/reports/" + report.getId()))
        .andExpect(jsonPath("$._links.reports.href").value("http://localhost/api/v1"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnNotFoundWhenReportDoesNotExist() throws Exception {
    String nonExistentReportId = UUID.randomUUID().toString();

    mockMvc.perform(get(API_V1_REPORTS_ID, nonExistentReportId)).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void findByAgentId_shouldReturnEmptyListWithHateoasLinksWhenNoReports() throws Exception {
    mockMvc
        .perform(get(API_V1_AGENTS_REPORTS, testAgentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1"));
  }

  @Test
  @WithUserDetails("admin")
  void findByAgentId_shouldReturnAllReportsForAgentWithHateoasLinks() throws Exception {
    Report report1 = new Report();
    Report report2 = new Report();
    testAgent.addReport(report1);
    testAgent.addReport(report2);
    mockMvc
        .perform(get(API_V1_AGENTS_REPORTS, testAgentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.reports").isArray())
        .andExpect(jsonPath("$._embedded.reports.length()").value(2))
        .andExpect(jsonPath("$._embedded.reports[?(@.id == '" + report1.getId() + "')]").exists())
        .andExpect(jsonPath("$._embedded.reports[?(@.id == '" + report2.getId() + "')]").exists())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1"))
        .andExpect(jsonPath("$._embedded.reports[0]._links.self.href").exists())
        .andExpect(jsonPath("$._embedded.reports[1]._links.self.href").exists());
  }

  @Test
  @WithUserDetails("admin")
  void findByAgentId_shouldNotReturnReportsFromOtherAgents() throws Exception {
    String otherAgentId = UUID.randomUUID().toString();
    Agent otherAgent = new Agent(otherAgentId);
    otherAgent.addReport(new Report());
    Agent testAgent = agentRepository.findById(testAgentId).get();
    testAgent.addReport(new Report());
    agentRepository.save(testAgent);
    String reportId = testAgent.getReports().getFirst().getId();
    mockMvc
        .perform(get(API_V1_AGENTS_REPORTS, testAgentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.reports.length()").value(1))
        .andExpect(jsonPath("$._embedded.reports[0].id").value(reportId))
        .andExpect(jsonPath("$._embedded.reports[0].agentId").value(testAgentId));
  }

  @Test
  @WithUserDetails("admin")
  void endToEndFlow_createAndRetrieveReport() throws Exception {
    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO("hash1", 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    String responseContent =
        mockMvc
            .perform(
                post(API_V1_AGENTS_REPORTS, testAgentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String reportId = objectMapper.readTree(responseContent).get("id").asText();

    mockMvc
        .perform(get(API_V1_REPORTS_ID, reportId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reportId))
        .andExpect(jsonPath("$.agentId").value(testAgentId));

    mockMvc
        .perform(get(API_V1_AGENTS_REPORTS, testAgentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.reports[?(@.id == '" + reportId + "')]").exists());
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldGenerateUniqueIdsForMultipleReports() throws Exception {
    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO("hash1", 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    String response1 =
        mockMvc
            .perform(
                post(API_V1_AGENTS_REPORTS, testAgentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String response2 =
        mockMvc
            .perform(
                post(API_V1_AGENTS_REPORTS, testAgentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String reportId1 = objectMapper.readTree(response1).get("id").asText();
    String reportId2 = objectMapper.readTree(response2).get("id").asText();

    assertNotEquals(reportId1, reportId2);
  }

  @Test
  void create_shouldRequireAuthentication() throws Exception {
    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO("hash1", 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void findById_shouldRequireAuthentication() throws Exception {
    Report report = new Report();
    reportRepository.save(report);
    mockMvc.perform(get(API_V1_REPORTS_ID, report.getId())).andExpect(status().isUnauthorized());
  }

  @Test
  void findByAgentId_shouldRequireAuthentication() throws Exception {
    mockMvc.perform(get(API_V1_AGENTS_REPORTS, testAgentId)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldAllowAdminRole() throws Exception {
    Report report = new Report();
    reportRepository.save(report);
    mockMvc
        .perform(get(API_V1_REPORTS_ID, report.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(report.getId()));
  }

  @Test
  @WithUserDetails("admin")
  void findByAgentId_shouldAllowAdminRole() throws Exception {
    testAgent.addReport(new Report());
    mockMvc
        .perform(get(API_V1_AGENTS_REPORTS, testAgentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.reports.length()").value(1));
  }

  @Test
  @Disabled
  @WithMockUser(roles = "USER")
  void create_shouldReturnForbiddenForNonAdminUserNotLinkedToAgent() throws Exception {
    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO("hash1", 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);
    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldCreateQualityCheckIfNotExists() throws Exception {
    String newHash = "new-quality-check-hash";
    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO(newHash, 0.85));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    assertTrue(qualityCheckRepository.findById(newHash).isPresent());
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldReuseExistingQualityCheck() throws Exception {
    QualityCheck existingCheck =
        new QualityCheck("existing-hash", "Test Check", "Test Description");
    qualityCheckRepository.save(existingCheck);

    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO("existing-hash", 0.92));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    assertEquals(1, qualityCheckRepository.count());
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldPersistMultipleResultsCorrectly() throws Exception {
    List<QualityCheckResultDTO> results =
        List.of(
            new QualityCheckResultDTO("check1", 0.95),
            new QualityCheckResultDTO("check2", 0.87),
            new QualityCheckResultDTO("check3", 0.78));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    String responseContent =
        mockMvc
            .perform(
                post(API_V1_AGENTS_REPORTS, testAgentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String reportId = objectMapper.readTree(responseContent).get("id").asText();

    Report savedReport = reportRepository.findById(reportId).orElseThrow();
    assertEquals(3, savedReport.getQualityCheckResults().size());
  }

  @Test
  @WithUserDetails("admin")
  void findById_shouldReturnReportWithResults() throws Exception {
    QualityCheck check1 = new QualityCheck("check1", "Check 1", "Description 1");
    QualityCheck check2 = new QualityCheck("check2", "Check 2", "Description 2");
    qualityCheckRepository.save(check1);
    qualityCheckRepository.save(check2);

    Report report = new Report();
    report.addQualityCheckResult(check1, 0.95);
    report.addQualityCheckResult(check2, 0.87);
    reportRepository.save(report);

    mockMvc
        .perform(get(API_V1_REPORTS_ID, report.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(report.getId()))
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.results.length()").value(2))
        .andExpect(jsonPath("$.results[?(@.hash == 'check1')].result").value(0.95))
        .andExpect(jsonPath("$.results[?(@.hash == 'check2')].result").value(0.87));
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldReturnNameWhenProvided() throws Exception {
    List<QualityCheckResultDTO> results =
        List.of(
            new QualityCheckResultDTO("check1", "Completeness Check", 0.95),
            new QualityCheckResultDTO("check2", "Consistency Check", 0.87));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.results.length()").value(2))
        .andExpect(jsonPath("$.results[?(@.hash == 'check1')].name").value("Completeness Check"))
        .andExpect(jsonPath("$.results[?(@.hash == 'check1')].result").value(0.95))
        .andExpect(jsonPath("$.results[?(@.hash == 'check2')].name").value("Consistency Check"))
        .andExpect(jsonPath("$.results[?(@.hash == 'check2')].result").value(0.87));
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldNotUpdateExistingQualityCheckName() throws Exception {
    // Create a quality check with an initial name
    QualityCheck existingCheck =
        new QualityCheck("existing-check", "Original Name", "Original Description");
    qualityCheckRepository.save(existingCheck);

    // Attempt to create a report with a different name for the same hash
    List<QualityCheckResultDTO> results =
        List.of(new QualityCheckResultDTO("existing-check", "Updated Name", 0.85));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.results[0].hash").value("existing-check"))
        .andExpect(jsonPath("$.results[0].name").value("Original Name"))
        .andExpect(jsonPath("$.results[0].result").value(0.85));

    // Verify the quality check name was not updated in the database
    QualityCheck savedCheck = qualityCheckRepository.findById("existing-check").orElseThrow();
    assertEquals("Original Name", savedCheck.getName());
    assertNotEquals("Updated Name", savedCheck.getName());
  }

  @Test
  @Disabled
  @WithUserDetails("admin")
  void create_shouldValidateHashFormat() throws Exception {
    List<QualityCheckResultDTO> results =
        List.of(new QualityCheckResultDTO("invalid hash with spaces", 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Disabled
  @WithUserDetails("admin")
  void create_shouldRejectHashWithSpecialCharacters() throws Exception {
    List<QualityCheckResultDTO> results =
        List.of(new QualityCheckResultDTO("hash@with$special#chars", 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldAcceptHashWithUnderscoresAndHyphens() throws Exception {
    List<QualityCheckResultDTO> results =
        List.of(new QualityCheckResultDTO("valid-hash_with_123", 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.results[0].hash").value("valid-hash_with_123"))
        .andExpect(jsonPath("$.results[0].result").value(0.95));
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldRejectHashThatIsTooLong() throws Exception {
    String longHash = "a".repeat(256);
    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO(longHash, 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldAcceptValidHashAtMaxLength() throws Exception {
    String maxLengthHash = "a".repeat(255);
    List<QualityCheckResultDTO> results = List.of(new QualityCheckResultDTO(maxLengthHash, 0.95));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.results[0].hash").value(maxLengthHash));
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldRejectNullResultValue() throws Exception {
    String jsonRequest =
        """
        {
          "results": [
            {
              "hash": "test-hash",
              "result": null
            }
          ]
        }
        """;

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void create_shouldRejectBlankHash() throws Exception {
    String jsonRequest =
        """
        {
          "results": [
            {
              "hash": "",
              "result": 0.95
            }
          ]
        }
        """;

    mockMvc
        .perform(
            post(API_V1_AGENTS_REPORTS, testAgentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void endToEndFlow_createReportWithResultsAndRetrieve() throws Exception {
    List<QualityCheckResultDTO> results =
        List.of(
            new QualityCheckResultDTO("completeness-check", 0.98),
            new QualityCheckResultDTO("consistency-check", 0.85),
            new QualityCheckResultDTO("accuracy-check", 0.92));
    ReportCreateRequest createRequest = new ReportCreateRequest(results);

    String responseContent =
        mockMvc
            .perform(
                post(API_V1_AGENTS_REPORTS, testAgentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.results.length()").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String reportId = objectMapper.readTree(responseContent).get("id").asText();

    mockMvc
        .perform(get(API_V1_REPORTS_ID, reportId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reportId))
        .andExpect(jsonPath("$.results.length()").value(3))
        .andExpect(jsonPath("$.results[?(@.hash == 'completeness-check')].result").value(0.98))
        .andExpect(jsonPath("$.results[?(@.hash == 'consistency-check')].result").value(0.85))
        .andExpect(jsonPath("$.results[?(@.hash == 'accuracy-check')].result").value(0.92));

    assertTrue(qualityCheckRepository.findById("completeness-check").isPresent());
    assertTrue(qualityCheckRepository.findById("consistency-check").isPresent());
    assertTrue(qualityCheckRepository.findById("accuracy-check").isPresent());
  }
}
