package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.domain.QueryType;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import java.io.StringReader;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ReportDqvServiceImplTest {

  private static final String AGENT_ID = "agent-42";
  private static final String TIMESTAMP = "2026-01-15T10:15:30Z";

  private ReportRepository reportRepository;
  private AgentRepository agentRepository;
  private ReportDqvServiceImpl service;

  @BeforeEach
  void setUp() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    reportRepository = Mockito.mock(ReportRepository.class);
    agentRepository = Mockito.mock(AgentRepository.class);
    service = new ReportDqvServiceImpl(reportRepository, agentRepository);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void exportDqv_shouldProduceGoldenTurtle() {
    Category completeness = category(1L, "Completeness");

    QualityCheck missingBirthDate =
        check(
            1L,
            "Missing birth date",
            "Share of records missing a birth date.",
            2.0,
            10.0,
            completeness,
            Set.of("privacy", "birth"));
    QualityCheckVersion birthV2 = version(missingBirthDate, 2, "abc123", QueryType.CQL);

    QualityCheck sampleConsistency = check(2L, "Sample consistency", "", 1.0, 2.0, null, Set.of());
    QualityCheckVersion consistencyV1 = version(sampleConsistency, 1, "def456", QueryType.UNKNOWN);

    QualityCheck donorCoverage =
        check(3L, "Donor coverage", null, 5.0, 15.0, completeness, Set.of());
    QualityCheckVersion coverageV1 = version(donorCoverage, 1, "ghi789", QueryType.UNKNOWN);

    Report report = new Report();
    report.setAgentId(AGENT_ID);
    report.setTotalPatients(150);
    report.setTotalSamples(420);
    report.addQualityCheckResult(birthV2, 0.05);
    report.addQualityCheckResult(consistencyV1, 0.03);
    report.addQualityCheckResult(coverageV1, null);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));

    Agent agent = new Agent(AGENT_ID);
    agent.setName("Brno Site");
    agent.setVersion("1.4.2");
    when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));

    String turtle = service.exportDqv(report.getId());

    String expectedTurtle =
        """
            @prefix dqv:            <http://www.w3.org/ns/dqv#> .
            @prefix oa:             <http://www.w3.org/ns/oa#> .
            @prefix prov:           <http://www.w3.org/ns/prov#> .
            @prefix dcat:           <http://www.w3.org/ns/dcat#> .
            @prefix dcterms:        <http://purl.org/dc/terms/> .
            @prefix skos:           <http://www.w3.org/2004/02/skos/core#> .
            @prefix owl:            <http://www.w3.org/2002/07/owl#> .
            @prefix rdfs:           <http://www.w3.org/2000/01/rdf-schema#> .
            @prefix qb:             <http://purl.org/linked-data/cube#> .
            @prefix xsd:            <http://www.w3.org/2001/XMLSchema#> .
            @prefix om:             <http://www.ontology-of-units-of-measure.org/resource/om-2/> .
            @prefix sdmx-attribute: <http://purl.org/linked-data/sdmx/2009/attribute#> .
            @prefix fdqf:           <https://github.com/bbmri-cz/data-quality-framework/ns/fdqf#> .
            @base <http://localhost/api/v1/> .

            <reports/__REPORT_ID__>
                a                     dqv:QualityMeasurementDataset , dqv:QualityMetadata;
                dcterms:title         "Data quality report __REPORT_ID__"@en;
                dcterms:description   "Values are differentially private estimates computed at the data source."@en;
                prov:generatedAtTime  "2026-01-15T10:15:30Z"^^xsd:dateTime;
                prov:wasAttributedTo  <agents/agent-42#agent>;
                dcterms:hasPart       <reports/__REPORT_ID__#m-patients> , <reports/__REPORT_ID__#m-samples> ,
                                      <reports/__REPORT_ID__#m-1-v2> , <reports/__REPORT_ID__#m-2-v1> ,
                                      <reports/__REPORT_ID__#s-1-v2> , <reports/__REPORT_ID__#s-2-v1> ,
                                      <reports/__REPORT_ID__#s-3-v1> .

            <agents/agent-42#agent>
                a                prov:SoftwareAgent;
                rdfs:label       "Brno Site"@en;
                owl:versionInfo  "1.4.2" .

            <agents/agent-42#dataset>
                a                          dcat:Dataset;
                dcterms:title              "Brno Site"@en;
                dqv:hasQualityMeasurement  <reports/__REPORT_ID__#m-patients> , <reports/__REPORT_ID__#m-samples> ,
                                           <reports/__REPORT_ID__#m-1-v2> , <reports/__REPORT_ID__#m-2-v1>;
                dqv:hasQualityAnnotation   <reports/__REPORT_ID__#s-1-v2> , <reports/__REPORT_ID__#s-2-v1> ,
                                           <reports/__REPORT_ID__#s-3-v1> .

            <categories>  a  skos:ConceptScheme .

            <categories/1>
                a               dqv:Dimension;
                skos:prefLabel  "Completeness"@en;
                skos:inScheme   <categories> .

            fdqf:volume  a  dqv:Dimension .

            fdqf:patientCount  a     dqv:Metric;
                dqv:inDimension  fdqf:volume .

            fdqf:sampleCount  a     dqv:Metric;
                dqv:inDimension  fdqf:volume .

            fdqf:uncategorised  a  dqv:Dimension .

            <quality-checks/1>  dcat:keyword  "birth" , "privacy" .

            <quality-checks/1/versions/2>
                a                      dqv:Metric;
                skos:prefLabel         "Missing birth date"@en;
                skos:definition        "Share of records missing a birth date."@en;
                dqv:expectedDataType   xsd:double;
                dqv:inDimension        <categories/1>;
                dcterms:isVersionOf    <quality-checks/1>;
                owl:versionInfo        "2";
                dcterms:identifier     "abc123";
                fdqf:queryLanguage     "CQL";
                fdqf:warningThreshold  "0.02"^^xsd:double;
                fdqf:errorThreshold    "0.1"^^xsd:double .

            <quality-checks/2/versions/1>
                a                      dqv:Metric;
                skos:prefLabel         "Sample consistency"@en;
                dqv:expectedDataType   xsd:double;
                dqv:inDimension        fdqf:uncategorised;
                dcterms:isVersionOf    <quality-checks/2>;
                owl:versionInfo        "1";
                dcterms:identifier     "def456";
                fdqf:warningThreshold  "0.01"^^xsd:double;
                fdqf:errorThreshold    "0.02"^^xsd:double .

            <quality-checks/3/versions/1>
                a                      dqv:Metric;
                skos:prefLabel         "Donor coverage"@en;
                dqv:expectedDataType   xsd:double;
                dqv:inDimension        <categories/1>;
                dcterms:isVersionOf    <quality-checks/3>;
                owl:versionInfo        "1";
                dcterms:identifier     "ghi789";
                fdqf:warningThreshold  "0.05"^^xsd:double;
                fdqf:errorThreshold    "0.15"^^xsd:double .

            <reports/__REPORT_ID__#m-patients>
                a                    dqv:QualityMeasurement;
                qb:dataSet           <reports/__REPORT_ID__>;
                dqv:computedOn       <agents/agent-42#dataset>;
                dqv:isMeasurementOf  fdqf:patientCount;
                dqv:value            "150"^^xsd:integer;
                rdfs:comment         "Rounded up to the nearest 10."@en .

            <reports/__REPORT_ID__#m-samples>
                a                    dqv:QualityMeasurement;
                qb:dataSet           <reports/__REPORT_ID__>;
                dqv:computedOn       <agents/agent-42#dataset>;
                dqv:isMeasurementOf  fdqf:sampleCount;
                dqv:value            "420"^^xsd:integer;
                rdfs:comment         "Rounded up to the nearest 10."@en .

            <reports/__REPORT_ID__#m-1-v2>
                a                           dqv:QualityMeasurement;
                qb:dataSet                  <reports/__REPORT_ID__>;
                dqv:computedOn              <agents/agent-42#dataset>;
                dqv:isMeasurementOf         <quality-checks/1/versions/2>;
                dqv:value                   "0.05"^^xsd:double;
                sdmx-attribute:unitMeasure  om:one .

            <reports/__REPORT_ID__#m-2-v1>
                a                           dqv:QualityMeasurement;
                qb:dataSet                  <reports/__REPORT_ID__>;
                dqv:computedOn              <agents/agent-42#dataset>;
                dqv:isMeasurementOf         <quality-checks/2/versions/1>;
                dqv:value                   "0.03"^^xsd:double;
                sdmx-attribute:unitMeasure  om:one .

            <reports/__REPORT_ID__#s-1-v2>
                a                    dqv:QualityAnnotation;
                oa:motivatedBy       dqv:qualityAssessment , oa:classifying;
                oa:hasTarget         <agents/agent-42#dataset>;
                oa:hasBody           fdqf:warning;
                prov:wasDerivedFrom  <reports/__REPORT_ID__#m-1-v2> .

            <reports/__REPORT_ID__#s-2-v1>
                a                    dqv:QualityAnnotation;
                oa:motivatedBy       dqv:qualityAssessment , oa:classifying;
                oa:hasTarget         <agents/agent-42#dataset>;
                oa:hasBody           fdqf:failed;
                prov:wasDerivedFrom  <reports/__REPORT_ID__#m-2-v1> .

            <reports/__REPORT_ID__#s-3-v1>
                a                    dqv:QualityAnnotation;
                oa:motivatedBy       dqv:qualityAssessment , oa:classifying;
                oa:hasTarget         <agents/agent-42#dataset>;
                oa:hasBody           fdqf:notComputed;
                prov:wasDerivedFrom  <quality-checks/3/versions/1> .
            """
            .replace("__REPORT_ID__", report.getId());

    Model expected = parse(expectedTurtle);
    Model actual = parse(turtle);
    assertTrue(
        actual.isIsomorphicWith(expected),
        "Exported Turtle does not match the expected model.\nActual:\n" + turtle);
  }

  @Test
  void exportDqv_shouldThrowWhenReportMissing() {
    when(reportRepository.findById("missing")).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> service.exportDqv("missing"));
  }

  @Test
  void exportDqv_shouldOmitMeasurementsWhenTotalsAndResultsMissing() {
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    Agent agent = new Agent(AGENT_ID);
    agent.setName("Brno Site");
    when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));

    Model model = parse(service.exportDqv(report.getId()));

    assertTrue(
        model
            .listStatements(
                null,
                model.createProperty("http://www.w3.org/ns/dqv#hasQualityMeasurement"),
                (org.apache.jena.rdf.model.RDFNode) null)
            .toList()
            .isEmpty());
    assertTrue(
        model
            .listStatements(
                null,
                model.createProperty("http://www.w3.org/ns/dqv#hasQualityAnnotation"),
                (org.apache.jena.rdf.model.RDFNode) null)
            .toList()
            .isEmpty());
  }

  @Test
  void exportDqv_shouldFallBackToAgentIdWhenAgentMissing() {
    Report report = new Report();
    report.setAgentId("missing-agent");
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById("missing-agent")).thenReturn(Optional.empty());

    Model model = parse(service.exportDqv(report.getId()));

    var agentResource = model.getResource("http://localhost/api/v1/agents/missing-agent#agent");
    assertEquals(2, agentResource.listProperties().toList().size());
    assertEquals(
        "missing-agent",
        agentResource.getProperty(org.apache.jena.vocabulary.RDFS.label).getString());
  }

  @Test
  void exportDqv_shouldEscapeSpecialCharactersInLiterals() {
    QualityCheck quoted = check(1L, "Odd \"quoted\" \\ check", null, 0.0, 0.0, null, Set.of());
    QualityCheckVersion version = version(quoted, 1, "hash", QueryType.UNKNOWN);
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    report.addQualityCheckResult(version, 0.0);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

    Model model = parse(service.exportDqv(report.getId()));

    assertEquals(
        "Odd \"quoted\" \\ check",
        model
            .listObjectsOfProperty(
                model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel"))
            .next()
            .asLiteral()
            .getString());
  }

  private static Category category(long id, String name) {
    Category category = new Category(name, "#FF5733");
    ReflectionTestUtils.setField(category, "id", id);
    return category;
  }

  private static QualityCheck check(
      long id,
      String name,
      String description,
      double warningThreshold,
      double errorThreshold,
      Category category,
      Set<String> keywords) {
    QualityCheck check =
        new QualityCheck(name, description, warningThreshold, errorThreshold, category);
    ReflectionTestUtils.setField(check, "id", id);
    check.setKeywords(keywords);
    return check;
  }

  private static QualityCheckVersion version(
      QualityCheck check, int number, String hash, QueryType type) {
    QualityCheckVersion version = new QualityCheckVersion(check, number, "SELECT 1", hash, type);
    check.addVersion(version);
    return version;
  }

  private static Model parse(String turtle) {
    Model model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, new StringReader(turtle), null, Lang.TURTLE);
    return model;
  }
}
