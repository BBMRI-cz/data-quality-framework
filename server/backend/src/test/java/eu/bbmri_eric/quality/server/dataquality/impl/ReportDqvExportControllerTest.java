package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.domain.QueryType;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ReportDqvExportControllerTest {

  private static final String API_V1_REPORTS_ID = "/api/v1/reports/{id}";
  private static final String API_V1_REPORTS_ID_EXPORT = "/api/v1/reports/{id}/export";
  private static final String TEXT_TURTLE = "text/turtle";

  @Autowired private MockMvc mockMvc;
  @Autowired private ReportRepository reportRepository;
  @Autowired private AgentRepository agentRepository;
  @Autowired private QualityCheckRepository qualityCheckRepository;

  private Agent testAgent;

  @BeforeEach
  void setUp() {
    agentRepository.deleteAll();
    testAgent = agentRepository.save(new Agent(UUID.randomUUID().toString()));
    testAgent.setName("Brno Site");
    testAgent.setVersion("1.4.2");
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void exportDqv_shouldReturnTurtleDocumentForHumanUser() throws Exception {
    Report report = seedReportWithResult();

    mockMvc
        .perform(get(API_V1_REPORTS_ID_EXPORT, report.getId()).accept(TEXT_TURTLE))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, TEXT_TURTLE))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("PREFIX dqv:")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("dqv:QualityMeasurementDataset")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("fdqf:queryLanguage")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"CQL\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"0.05\"^^xsd:double")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("#m-")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("#s-")));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void exportDqv_shouldReturnTurtleForWildcardAccept() throws Exception {
    Report report = seedReportWithResult();

    mockMvc
        .perform(get(API_V1_REPORTS_ID_EXPORT, report.getId()).accept(MediaType.ALL))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, TEXT_TURTLE));
  }

  @Test
  void exportDqv_shouldRequireAuthentication() throws Exception {
    Report report = new Report();
    reportRepository.save(report);

    mockMvc
        .perform(get(API_V1_REPORTS_ID_EXPORT, report.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "USER")
  void exportDqv_shouldReturnForbiddenForUserWithoutHumanUserOrAdminRole() throws Exception {
    Report report = new Report();
    reportRepository.save(report);

    mockMvc
        .perform(get(API_V1_REPORTS_ID_EXPORT, report.getId()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void exportDqv_shouldReturnNotFoundForUnknownReport() throws Exception {
    mockMvc
        .perform(get(API_V1_REPORTS_ID_EXPORT, UUID.randomUUID().toString()).accept(TEXT_TURTLE))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldStillReturnHalJsonForWildcardAccept() throws Exception {
    Report report = seedReportWithResult();

    mockMvc
        .perform(get(API_V1_REPORTS_ID, report.getId()).accept(MediaType.ALL))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("json")))
        .andExpect(jsonPath("$.id").value(report.getId()))
        .andExpect(jsonPath("$._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldStillReturnHalJsonForJsonAndHalAccepts() throws Exception {
    Report report = seedReportWithResult();

    for (String accept : new String[] {MediaType.APPLICATION_JSON_VALUE, "application/hal+json"}) {
      mockMvc
          .perform(get(API_V1_REPORTS_ID, report.getId()).accept(accept))
          .andExpect(status().isOk())
          .andExpect(
              header()
                  .string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("json")))
          .andExpect(jsonPath("$.id").value(report.getId()))
          .andExpect(jsonPath("$._links.self.href").exists());
    }
  }

  private Report seedReportWithResult() {
    QualityCheck check =
        new QualityCheck("Missing birth date", "Share missing birth date.", 2.0, 10.0);
    QualityCheckVersion version =
        new QualityCheckVersion(check, 1, "SELECT 1", "abc123", QueryType.CQL);
    check.addVersion(version);
    qualityCheckRepository.save(check);

    Report report = new Report();
    report.setTotalPatients(150);
    report.addQualityCheckResult(version, 0.05);
    testAgent.addReport(report);
    agentRepository.saveAndFlush(testAgent);
    return report;
  }
}
