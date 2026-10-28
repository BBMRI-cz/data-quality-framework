package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ReportDqvServiceImplTest {

  private static final String AGENT_ID = "agent-42";
  private static final String TIMESTAMP = "2026-01-15T10:15:30Z";
  private static final String NS_DQV = "http://www.w3.org/ns/dqv#";
  private static final String NS_OA = "http://www.w3.org/ns/oa#";
  private static final String NS_PROV = "http://www.w3.org/ns/prov#";
  private static final String NS_FDQF =
      "https://github.com/bbmri-cz/data-quality-framework/ns/fdqf#";

  private ReportRepository reportRepository;
  private AgentRepository agentRepository;
  private ReportDqvServiceImpl service;

  @BeforeEach
  void setUp() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    reportRepository = Mockito.mock(ReportRepository.class);
    agentRepository = Mockito.mock(AgentRepository.class);
    ObjectProvider<BuildProperties> buildPropertiesProvider = Mockito.mock(ObjectProvider.class);
    when(buildPropertiesProvider.getIfAvailable()).thenReturn(null);
    service =
        new ReportDqvServiceImpl(reportRepository, agentRepository, buildPropertiesProvider, "");
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
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));

    String turtle = service.exportDqv(report.getId());

    String expectedTurtle =
        """
            @prefix rdf:            <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
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
                rdfs:label       "Brno Site";
                owl:versionInfo  "1.4.2" .

            <agents/agent-42#dataset>
                a                          dcat:Dataset;
                dcterms:title              "Brno Site";
                dqv:hasQualityMeasurement  <reports/__REPORT_ID__#m-patients> , <reports/__REPORT_ID__#m-samples> ,
                                           <reports/__REPORT_ID__#m-1-v2> , <reports/__REPORT_ID__#m-2-v1>;
                dqv:hasQualityAnnotation   <reports/__REPORT_ID__#s-1-v2> , <reports/__REPORT_ID__#s-2-v1> ,
                                           <reports/__REPORT_ID__#s-3-v1> .

            <#server>
                a           prov:SoftwareAgent;
                rdfs:label  "Data Quality Server"@en .

            <categories>  a  skos:ConceptScheme .

            <categories/1>
                a               dqv:Dimension;
                skos:prefLabel  "Completeness";
                skos:inScheme   <categories> .

            fdqf:volume  a  dqv:Dimension .

            fdqf:patientCount  a     dqv:Metric;
                dqv:inDimension       fdqf:volume;
                dqv:expectedDataType  xsd:integer;
                skos:prefLabel        "Patient count"@en .

            fdqf:sampleCount  a     dqv:Metric;
                dqv:inDimension       fdqf:volume;
                dqv:expectedDataType  xsd:integer;
                skos:prefLabel        "Sample count"@en .

            fdqf:uncategorised  a  dqv:Dimension .

            fdqf:statuses  a  skos:ConceptScheme .

            fdqf:warning  a     skos:Concept;
                skos:prefLabel  "Warning"@en;
                skos:inScheme   fdqf:statuses .

            fdqf:failed  a     skos:Concept;
                skos:prefLabel  "Failed"@en;
                skos:inScheme   fdqf:statuses .

            fdqf:notComputed  a     skos:Concept;
                skos:prefLabel  "Not computed"@en;
                skos:inScheme   fdqf:statuses .

            fdqf:warningThreshold  a  rdf:Property , owl:DatatypeProperty;
                rdfs:range    xsd:double;
                rdfs:comment  "An affected fraction strictly greater than this value classifies the result as warning; higher fractions indicate worse quality."@en .

            fdqf:errorThreshold  a  rdf:Property , owl:DatatypeProperty;
                rdfs:range    xsd:double;
                rdfs:comment  "An affected fraction strictly greater than this value classifies the result as failed; higher fractions indicate worse quality."@en .

            fdqf:queryLanguage  a  rdf:Property , owl:DatatypeProperty;
                rdfs:range    xsd:string;
                rdfs:comment  "Query language of the metric's query text; the query text itself is never distributed."@en .

            <quality-checks/1>
                skos:prefLabel   "Missing birth date";
                skos:definition  "Share of records missing a birth date.";
                dcat:keyword     "birth" , "privacy" .

            <quality-checks/2>  skos:prefLabel  "Sample consistency" .

            <quality-checks/3>  skos:prefLabel  "Donor coverage" .

            <quality-checks/1/versions/2>
                a                     dqv:Metric;
                dqv:expectedDataType  xsd:double;
                dcterms:isVersionOf   <quality-checks/1>;
                owl:versionInfo       "2";
                dcterms:identifier    "abc123";
                fdqf:queryLanguage    "CQL";
                rdfs:comment          "Differentially private ratio: noisy counts below the agent's suppression threshold are reported as 0."@en .

            <quality-checks/2/versions/1>
                a                     dqv:Metric;
                dqv:expectedDataType  xsd:double;
                dcterms:isVersionOf   <quality-checks/2>;
                owl:versionInfo       "1";
                dcterms:identifier    "def456";
                rdfs:comment          "Differentially private ratio: noisy counts below the agent's suppression threshold are reported as 0."@en .

            <quality-checks/3/versions/1>
                a                     dqv:Metric;
                dqv:expectedDataType  xsd:double;
                dcterms:isVersionOf   <quality-checks/3>;
                owl:versionInfo       "1";
                dcterms:identifier    "ghi789";
                rdfs:comment          "Differentially private ratio: noisy counts below the agent's suppression threshold are reported as 0."@en .

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
                a                     dqv:QualityAnnotation;
                oa:motivatedBy        dqv:qualityAssessment , oa:classifying;
                oa:hasTarget          <agents/agent-42#dataset>;
                prov:wasAttributedTo  <#server>;
                fdqf:warningThreshold "0.02"^^xsd:double;
                fdqf:errorThreshold   "0.1"^^xsd:double;
                dqv:inDimension       <categories/1>;
                oa:hasBody            fdqf:warning;
                prov:wasDerivedFrom   <reports/__REPORT_ID__#m-1-v2> .

            <reports/__REPORT_ID__#s-2-v1>
                a                     dqv:QualityAnnotation;
                oa:motivatedBy        dqv:qualityAssessment , oa:classifying;
                oa:hasTarget          <agents/agent-42#dataset>;
                prov:wasAttributedTo  <#server>;
                fdqf:warningThreshold "0.01"^^xsd:double;
                fdqf:errorThreshold   "0.02"^^xsd:double;
                dqv:inDimension       fdqf:uncategorised;
                oa:hasBody            fdqf:failed;
                prov:wasDerivedFrom   <reports/__REPORT_ID__#m-2-v1> .

            <reports/__REPORT_ID__#s-3-v1>
                a                     dqv:QualityAnnotation;
                oa:motivatedBy        dqv:qualityAssessment , oa:classifying;
                oa:hasTarget          <agents/agent-42#dataset>;
                prov:wasAttributedTo  <#server>;
                fdqf:warningThreshold "0.05"^^xsd:double;
                fdqf:errorThreshold   "0.15"^^xsd:double;
                dqv:inDimension       <categories/1>;
                oa:hasBody            fdqf:notComputed;
                prov:wasDerivedFrom   <quality-checks/3/versions/1> .
            """
            .replace("__REPORT_ID__", report.getId());

    Model expected = parse(expectedTurtle);
    Model actual = stripExportTimes(parse(turtle));
    assertTrue(
        actual.isIsomorphicWith(expected),
        "Exported Turtle does not match the expected model.\nActual:\n" + turtle);
  }

  @Test
  void exportDqv_shouldThrowWhenReportMissing() {
    when(reportRepository.findWithDetailsById("missing")).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> service.exportDqv("missing"));
  }

  @Test
  void exportDqv_shouldOmitMeasurementsWhenTotalsAndResultsMissing() {
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    Agent agent = new Agent(AGENT_ID);
    agent.setName("Brno Site");
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));

    Model model = parse(service.exportDqv(report.getId()));

    assertTrue(
        model
            .listStatements(null, RDF.type, model.createResource(NS_DQV + "QualityMeasurement"))
            .toList()
            .isEmpty());
    assertTrue(
        model
            .listStatements(null, RDF.type, model.createResource(NS_DQV + "QualityAnnotation"))
            .toList()
            .isEmpty());
  }

  @Test
  void exportDqv_shouldFallBackToAgentIdWhenAgentMissing() {
    Report report = new Report();
    report.setAgentId("missing-agent");
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById("missing-agent")).thenReturn(Optional.empty());

    Model model = parse(service.exportDqv(report.getId()));

    Resource agentResource =
        model.getResource("http://localhost/api/v1/agents/missing-agent#agent");
    assertEquals(2, agentResource.listProperties().toList().size());
    assertEquals("missing-agent", agentResource.getProperty(RDFS.label).getString());
  }

  @Test
  void exportDqv_shouldEscapeSpecialCharactersInLiterals() {
    QualityCheck quoted = check(1L, "Odd \"quoted\" \\ check", null, 0.0, 0.0, null, Set.of());
    QualityCheckVersion version = version(quoted, 1, "hash", QueryType.UNKNOWN);
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    report.addQualityCheckResult(version, 0.0);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

    Model model = parse(service.exportDqv(report.getId()));

    assertEquals(
        "Odd \"quoted\" \\ check",
        model
            .getResource("http://localhost/api/v1/quality-checks/1")
            .getProperty(model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel"))
            .getString());
  }

  @Test
  void exportDqv_shouldTreatNonFiniteResultsAsNotComputed() {
    QualityCheck check = check(1L, "Check", null, 1.0, 5.0, null, Set.of());
    QualityCheckVersion version = version(check, 1, "hash", QueryType.UNKNOWN);
    QualityCheck nanCheck = check(2L, "NaN check", null, Double.NaN, Double.NaN, null, Set.of());
    QualityCheckVersion nanVersion = version(nanCheck, 2, "hash2", QueryType.UNKNOWN);
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    report.addQualityCheckResult(version, Double.NaN);
    report.addQualityCheckResult(nanVersion, Double.POSITIVE_INFINITY);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

    Model model = parse(service.exportDqv(report.getId()));

    Resource notComputed = model.createResource(NS_FDQF + "notComputed");
    List<Resource> notComputedAnnotations =
        model
            .listSubjectsWithProperty(model.createProperty(NS_OA, "hasBody"), notComputed)
            .toList();
    assertEquals(2, notComputedAnnotations.size());
    assertTrue(
        model
            .listStatements(null, RDF.type, model.createResource(NS_DQV + "QualityMeasurement"))
            .toList()
            .isEmpty(),
        "Non-finite results must not produce a measurement");
  }

  @Test
  void exportDqv_shouldNotDowngradeAtExactThreshold() {
    QualityCheck check = check(1L, "Boundary", null, 5.0, 7.0, null, Set.of());
    QualityCheckVersion version = version(check, 1, "hash", QueryType.UNKNOWN);
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    report.addQualityCheckResult(version, 0.07);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

    Model model = parse(service.exportDqv(report.getId()));

    Resource warningBody = model.createResource(NS_FDQF + "warning");
    assertFalse(
        model
            .listSubjectsWithProperty(model.createProperty(NS_OA, "hasBody"), warningBody)
            .toList()
            .isEmpty(),
        "0.07 must not be classifier as failed when the thresholds are at the exact value");
  }

  @Test
  void exportDqv_shouldPercentEncodeUnreservedCharactersInAgentId() {
    String weirdAgentId = "agent 42#x";
    Report report = new Report();
    report.setAgentId(weirdAgentId);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(weirdAgentId)).thenReturn(Optional.empty());

    String turtle = service.exportDqv(report.getId());

    assertTrue(
        turtle.contains("agent%2042%23x"),
        "Agent ID characters must be percent-encoded in minted IRIs");
  }

  @Test
  void exportDqv_shouldSupportTwoVersionsOfOneCheck() {
    QualityCheck check = check(1L, "Multi", null, 1.0, 5.0, null, Set.of());
    QualityCheckVersion v1 = version(check, 1, "hash-v1", QueryType.SQL);
    QualityCheckVersion v2 = version(check, 2, "hash-v2", QueryType.SQL);
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    report.addQualityCheckResult(v1, 0.1);
    report.addQualityCheckResult(v2, 0.2);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

    Model model = parse(service.exportDqv(report.getId()));

    assertTrue(
        model
                .listSubjectsWithProperty(RDF.type, model.createResource(NS_DQV + "Metric"))
                .toList()
                .size()
            >= 2);
    assertTrue(
        model.contains(
            model.createResource("http://localhost/api/v1/quality-checks/1/versions/1"),
            RDF.type,
            model.createResource(NS_DQV + "Metric")));
    assertTrue(
        model.contains(
            model.createResource("http://localhost/api/v1/quality-checks/1/versions/2"),
            RDF.type,
            model.createResource(NS_DQV + "Metric")));
  }

  @Test
  void exportDqv_shouldNotExposeQueryText() {
    QualityCheck check = check(1L, "Secret", null, 1.0, 5.0, null, Set.of());
    QualityCheckVersion version =
        new QualityCheckVersion(check, 1, "SELECT top_secret FROM x", "hash", QueryType.SQL);
    check.addVersion(version);
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    report.addQualityCheckResult(version, 0.1);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

    String turtle = service.exportDqv(report.getId());

    assertFalse(turtle.contains("top_secret"), "Query text must never appear in the export");
  }

  @Test
  void exportDqv_shouldUseConfiguredBaseIriWhenSet() {
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());
    ReportDqvServiceImpl configuredService =
        new ReportDqvServiceImpl(
            reportRepository,
            agentRepository,
            Mockito.mock(ObjectProvider.class),
            "https://dq.example.org/fdqf");

    String turtle = configuredService.exportDqv(report.getId());

    assertTrue(turtle.contains("<https://dq.example.org/fdqf/>"), turtle);
  }

  @Test
  void exportDqv_shouldExposeNonFiniteThresholdsWithoutFailing() {
    QualityCheck check =
        check(1L, "Check", null, Double.NaN, Double.POSITIVE_INFINITY, null, Set.of());
    QualityCheckVersion version = version(check, 1, "hash", QueryType.UNKNOWN);
    Report report = new Report();
    report.setAgentId(AGENT_ID);
    report.addQualityCheckResult(version, 0.5);
    ReflectionTestUtils.setField(report, "timestamp", Instant.parse(TIMESTAMP));
    when(reportRepository.findWithDetailsById(report.getId())).thenReturn(Optional.of(report));
    when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

    Model model = parse(service.exportDqv(report.getId()));

    assertTrue(
        model
            .listObjectsOfProperty(model.createProperty(NS_FDQF, "warningThreshold"))
            .toList()
            .stream()
            .anyMatch(node -> node.isLiteral() && "NaN".equals(node.asLiteral().getString())));
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

  /**
   * Removes the per-annotation {@code prov:generatedAtTime} statements, which contain the
   * nondeterministic export timestamp, after verifying that every annotation carries one.
   */
  private static Model stripExportTimes(Model model) {
    Property generatedAt = model.createProperty(NS_PROV, "generatedAtTime");
    List<Resource> annotations =
        model
            .listSubjectsWithProperty(RDF.type, model.createResource(NS_DQV + "QualityAnnotation"))
            .toList();
    assertFalse(annotations.isEmpty(), "Expected at least one annotation");
    for (Resource annotation : annotations) {
      assertTrue(annotation.hasProperty(generatedAt), "Annotation must carry its export time");
      annotation.removeAll(generatedAt);
    }
    return model;
  }

  private static Model parse(String turtle) {
    Model model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, new StringReader(turtle), null, Lang.TURTLE);
    return model;
  }
}
