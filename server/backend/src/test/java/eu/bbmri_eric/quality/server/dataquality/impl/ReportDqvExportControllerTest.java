package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.domain.QueryType;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import java.io.StringReader;
import java.util.UUID;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ReportDqvExportControllerTest {

  private static final String API_V1_REPORTS_ID = "/api/v1/reports/{id}";
  private static final String API_V1_REPORTS_ID_EXPORT = "/api/v1/reports/{id}/export";
  private static final String TEXT_TURTLE = "text/turtle";
  private static final String NS_DQV = "http://www.w3.org/ns/dqv#";
  private static final String NS_FDQF =
      "https://github.com/bbmri-cz/data-quality-framework/ns/fdqf#";

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

    MvcResult result =
        mockMvc
            .perform(get(API_V1_REPORTS_ID_EXPORT, report.getId()).accept(TEXT_TURTLE))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/turtle;charset=UTF-8"))
            .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
            .andReturn();

    String body = result.getResponse().getContentAsString();
    Model model = parse(body);

    Resource reportResource = reportResource(model, report.getId());
    assertTrue(
        model.contains(
            reportResource, RDF.type, model.createResource(NS_DQV + "QualityMeasurementDataset")),
        "Report must be typed as dqv:QualityMeasurementDataset");
    assertTrue(
        model.contains(reportResource, RDF.type, model.createResource(NS_DQV + "QualityMetadata")),
        "Report must be typed as dqv:QualityMetadata");
    assertTrue(
        model
            .listObjectsOfProperty(model.createProperty(NS_FDQF, "queryLanguage"))
            .toList()
            .stream()
            .anyMatch(node -> node.isLiteral() && "CQL".equals(node.asLiteral().getString())),
        "Query language must be exported");
    assertTrue(
        model
            .listSubjectsWithProperty(RDF.type, model.createResource(NS_DQV + "QualityMeasurement"))
            .toList()
            .stream()
            .anyMatch(
                subject ->
                    subject.isURIResource()
                        && subject.getURI().startsWith(reportResource.getURI() + "#m-")),
        "A measurement must exist for the computed result");
    assertTrue(
        model
            .listSubjectsWithProperty(RDF.type, model.createResource(NS_DQV + "QualityAnnotation"))
            .toList()
            .stream()
            .anyMatch(
                subject ->
                    subject.isURIResource()
                        && subject.getURI().startsWith(reportResource.getURI() + "#s-")),
        "An annotation must exist for the result");
    assertFalse(
        model.listObjects().toList().stream()
            .filter(RDFNode::isLiteral)
            .map(node -> node.asLiteral().getString())
            .anyMatch(literal -> literal.contains("SELECT 1")),
        "Query text must never appear in the export");
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void exportDqv_shouldReturnTurtleForWildcardAndJsonAccepts() throws Exception {
    Report report = seedReportWithResult();

    for (String accept : new String[] {MediaType.ALL_VALUE, MediaType.APPLICATION_JSON_VALUE}) {
      mockMvc
          .perform(get(API_V1_REPORTS_ID_EXPORT, report.getId()).accept(accept))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/turtle;charset=UTF-8"));
    }
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
    for (String accept : new String[] {TEXT_TURTLE, MediaType.APPLICATION_JSON_VALUE}) {
      mockMvc
          .perform(get(API_V1_REPORTS_ID_EXPORT, UUID.randomUUID().toString()).accept(accept))
          .andExpect(status().isNotFound());
    }
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

  private static Resource reportResource(Model model, String reportId) {
    return model.getResource("http://localhost/api/v1/reports/" + reportId);
  }

  private static Model parse(String turtle) {
    Model model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, new StringReader(turtle), "http://localhost/api/v1/", Lang.TURTLE);
    return model;
  }
}
