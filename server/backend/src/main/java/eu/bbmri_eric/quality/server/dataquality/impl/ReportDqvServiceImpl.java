package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.ReportDqvService;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckKeyword;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckResult;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.domain.QueryType;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Service implementation for exporting reports in the W3C Data Quality Vocabulary (DQV).
 *
 * <p>A report is rendered as a {@code dqv:QualityMeasurementDataset}. Each computed check result
 * becomes a {@code dqv:QualityMeasurement}, and every result is additionally classified with a
 * {@code dqv:QualityAnnotation} (computed with the shared {@link CheckStatus} rule). Only
 * version-stable facts (query hash, query language) are attached to the metric IRI; mutable
 * editorial data (name, description, keywords) lives on the quality check, and the thresholds
 * applied during classification are attached to the annotation together with the export time, so
 * repeated exports of the same report never attribute conflicting values to the same metric.
 *
 * <p>Query texts are never serialized; only their hash and language are exposed. Results that are
 * not finite (e.g. a stored {@code NaN}) are exported as not-computed annotations instead of
 * breaking the export.
 */
@Service
class ReportDqvServiceImpl implements ReportDqvService {

  private static final Logger logger = LoggerFactory.getLogger(ReportDqvServiceImpl.class);

  private static final String NS_DQV = "http://www.w3.org/ns/dqv#";
  private static final String NS_OA = "http://www.w3.org/ns/oa#";
  private static final String NS_PROV = "http://www.w3.org/ns/prov#";
  private static final String NS_DCAT = "http://www.w3.org/ns/dcat#";
  private static final String NS_DCTERMS = "http://purl.org/dc/terms/";
  private static final String NS_SKOS = "http://www.w3.org/2004/02/skos/core#";
  private static final String NS_OWL = "http://www.w3.org/2002/07/owl#";
  private static final String NS_QB = "http://purl.org/linked-data/cube#";
  private static final String NS_XSD = "http://www.w3.org/2001/XMLSchema#";
  private static final String NS_OM = "http://www.ontology-of-units-of-measure.org/resource/om-2/";
  private static final String NS_SDMX_ATTRIBUTE =
      "http://purl.org/linked-data/sdmx/2009/attribute#";
  private static final String NS_FDQF =
      "https://github.com/bbmri-cz/data-quality-framework/ns/fdqf#";

  private static final String REPORT_DESCRIPTION =
      "Counts are rounded up to the nearest 10; check results are noisy ratios computed by the"
          + " agent.";
  private static final String ROUNDING_COMMENT = "Rounded up to the nearest 10.";
  private static final String SUPPRESSION_COMMENT =
      "Differentially private ratio: noisy counts below the agent's suppression threshold are"
          + " reported as 0.";
  private static final String WARNING_THRESHOLD_COMMENT =
      "An affected fraction strictly greater than this value classifies the result as warning;"
          + " higher fractions indicate worse quality.";
  private static final String ERROR_THRESHOLD_COMMENT =
      "An affected fraction strictly greater than this value classifies the result as failed;"
          + " higher fractions indicate worse quality.";
  private static final String QUERY_LANGUAGE_COMMENT =
      "Query language of the metric's query text; the query text itself is never distributed.";
  private static final String UNCATEGORISED_DIMENSION = "uncategorised";
  private static final String SERVER_LABEL = "Data Quality Server";

  private final ReportRepository reportRepository;
  private final AgentRepository agentRepository;
  private final ObjectProvider<BuildProperties> buildPropertiesProvider;
  private final String configuredBaseIri;

  public ReportDqvServiceImpl(
      ReportRepository reportRepository,
      AgentRepository agentRepository,
      ObjectProvider<BuildProperties> buildPropertiesProvider,
      @Value("${app.dqv.base-iri:}") String configuredBaseIri) {
    this.reportRepository = reportRepository;
    this.agentRepository = agentRepository;
    this.buildPropertiesProvider = buildPropertiesProvider;
    this.configuredBaseIri = configuredBaseIri;
  }

  @Override
  @Transactional(readOnly = true)
  public String exportDqv(String reportId) {
    Report report =
        reportRepository
            .findWithDetailsById(reportId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException("Report with ID %s not found".formatted(reportId)));
    Agent agent = agentRepository.findById(report.getAgentId()).orElse(null);
    String base = resolveBaseIri();
    Instant exportedAt = Instant.now();
    logger.debug("Exporting report {} as DQV (base IRI: {})", reportId, base);

    Model model = ModelFactory.createDefaultModel();
    registerPrefixes(model);
    List<QualityCheckResult> results = sortedResults(report);
    String exportId = Long.toString(exportedAt.toEpochMilli());
    addReport(model, base, report, results, exportId);
    addAgent(model, base, report, agent, results, exportId);
    addServerSoftware(model, base);
    addCategories(model, base, results);
    addVocabularyTerms(model, report, results);
    addChecksAndMetrics(model, base, results);
    addMeasurements(model, base, report, results);
    addAnnotations(model, base, report, results, exportedAt, exportId);
    return RDFWriter.source(model).lang(Lang.TURTLE).base(base).asString();
  }

  private String resolveBaseIri() {
    if (notBlank(configuredBaseIri)) {
      return configuredBaseIri.endsWith("/") ? configuredBaseIri : configuredBaseIri + "/";
    }
    return ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/v1/")
        .build()
        .toUriString();
  }

  private void addServerSoftware(Model model, String base) {
    Resource server = model.createResource(serverIri(base));
    server.addProperty(RDF.type, model.createResource(NS_PROV + "SoftwareAgent"));
    server.addProperty(RDFS.label, model.createLiteral(SERVER_LABEL, "en"));
    BuildProperties properties = buildPropertiesProvider.getIfAvailable();
    if (properties != null && notBlank(properties.getVersion())) {
      server.addProperty(
          model.createProperty(NS_OWL, "versionInfo"),
          model.createLiteral(properties.getVersion()));
    }
  }

  private static void registerPrefixes(Model model) {
    model.setNsPrefix("dqv", NS_DQV);
    model.setNsPrefix("oa", NS_OA);
    model.setNsPrefix("prov", NS_PROV);
    model.setNsPrefix("dcat", NS_DCAT);
    model.setNsPrefix("dcterms", NS_DCTERMS);
    model.setNsPrefix("skos", NS_SKOS);
    model.setNsPrefix("owl", NS_OWL);
    model.setNsPrefix("rdfs", RDFS.getURI());
    model.setNsPrefix("qb", NS_QB);
    model.setNsPrefix("xsd", NS_XSD);
    model.setNsPrefix("om", NS_OM);
    model.setNsPrefix("sdmx-attribute", NS_SDMX_ATTRIBUTE);
    model.setNsPrefix("fdqf", NS_FDQF);
  }

  private static void addReport(
      Model model, String base, Report report, List<QualityCheckResult> results, String exportId) {
    Resource reportResource = model.createResource(reportIri(base, report));
    reportResource.addProperty(
        RDF.type, model.createResource(NS_DQV + "QualityMeasurementDataset"));
    reportResource.addProperty(RDF.type, model.createResource(NS_DQV + "QualityMetadata"));
    reportResource.addProperty(
        model.createProperty(NS_DCTERMS, "title"),
        model.createLiteral("Data quality report " + report.getId(), "en"));
    reportResource.addProperty(
        model.createProperty(NS_DCTERMS, "description"),
        model.createLiteral(REPORT_DESCRIPTION, "en"));
    reportResource.addProperty(
        model.createProperty(NS_PROV, "generatedAtTime"),
        model.createTypedLiteral(report.getTimestamp().toString(), XSDDatatype.XSDdateTime));
    reportResource.addProperty(
        model.createProperty(NS_PROV, "wasAttributedTo"),
        model.createResource(agentIri(base, report.getAgentId())));
    for (String part : partIris(base, report, results, exportId)) {
      reportResource.addProperty(
          model.createProperty(NS_DCTERMS, "hasPart"), model.createResource(part));
    }
  }

  private static void addAgent(
      Model model,
      String base,
      Report report,
      Agent agent,
      List<QualityCheckResult> results,
      String exportId) {
    String label =
        agent != null && notBlank(agent.getName()) ? agent.getName() : report.getAgentId();
    Resource agentResource = model.createResource(agentIri(base, report.getAgentId()));
    agentResource.addProperty(RDF.type, model.createResource(NS_PROV + "SoftwareAgent"));
    agentResource.addProperty(RDFS.label, model.createLiteral(label));
    if (agent != null && notBlank(agent.getVersion())) {
      agentResource.addProperty(
          model.createProperty(NS_OWL, "versionInfo"), model.createLiteral(agent.getVersion()));
    }
    Resource datasetResource = model.createResource(datasetIri(base, report.getAgentId()));
    datasetResource.addProperty(RDF.type, model.createResource(NS_DCAT + "Dataset"));
    datasetResource.addProperty(
        model.createProperty(NS_DCTERMS, "title"), model.createLiteral(label));
    Property hasQualityMeasurement = model.createProperty(NS_DQV, "hasQualityMeasurement");
    for (String measurementIri : measurementIris(base, report, results)) {
      datasetResource.addProperty(hasQualityMeasurement, model.createResource(measurementIri));
    }
    Property hasQualityAnnotation = model.createProperty(NS_DQV, "hasQualityAnnotation");
    for (QualityCheckResult result : results) {
      datasetResource.addProperty(
          hasQualityAnnotation,
          model.createResource(annotationIri(base, report, result, exportId)));
    }
  }

  private static void addCategories(Model model, String base, List<QualityCheckResult> results) {
    Map<Long, Category> categoriesById = new TreeMap<>();
    for (QualityCheckResult result : results) {
      Category category = result.getQualityCheck().getCategory();
      if (category != null && category.getId() != null) {
        categoriesById.putIfAbsent(category.getId(), category);
      }
    }
    if (categoriesById.isEmpty()) {
      return;
    }
    model
        .createResource(categoriesIri(base))
        .addProperty(RDF.type, model.createResource(NS_SKOS + "ConceptScheme"));
    for (Category category : categoriesById.values()) {
      Resource categoryResource = model.createResource(categoryIri(base, category.getId()));
      categoryResource.addProperty(RDF.type, model.createResource(NS_DQV + "Dimension"));
      if (notBlank(category.getName())) {
        categoryResource.addProperty(
            model.createProperty(NS_SKOS, "prefLabel"), model.createLiteral(category.getName()));
      }
      categoryResource.addProperty(
          model.createProperty(NS_SKOS, "inScheme"), model.createResource(categoriesIri(base)));
    }
  }

  /**
   * Declares the FDQF vocabulary terms used by the document so that the export is self-describing.
   * Dimensions and metrics must be declared because DQV requires every {@code dqv:Metric} to be in
   * at least one {@code dqv:Dimension}; status bodies are declared as SKOS concepts in a scheme
   * following the DQV quality-level pattern.
   */
  private static void addVocabularyTerms(
      Model model, Report report, List<QualityCheckResult> results) {
    if (report.getTotalPatients() != null || report.getTotalSamples() != null) {
      model
          .createResource(NS_FDQF + "volume")
          .addProperty(RDF.type, model.createResource(NS_DQV + "Dimension"));
    }
    if (report.getTotalPatients() != null) {
      addCountMetricDeclaration(model, "patientCount", "Patient count");
    }
    if (report.getTotalSamples() != null) {
      addCountMetricDeclaration(model, "sampleCount", "Sample count");
    }
    boolean hasUncategorised =
        results.stream()
            .map(result -> result.getQualityCheck().getCategory())
            .anyMatch(category -> category == null || category.getId() == null);
    if (hasUncategorised) {
      model
          .createResource(NS_FDQF + UNCATEGORISED_DIMENSION)
          .addProperty(RDF.type, model.createResource(NS_DQV + "Dimension"));
    }
    if (!results.isEmpty()) {
      declareStatusTerms(model, results);
      declareThresholdProperty(model, "warningThreshold", WARNING_THRESHOLD_COMMENT);
      declareThresholdProperty(model, "errorThreshold", ERROR_THRESHOLD_COMMENT);
    }
    boolean hasTypedVersion =
        results.stream().anyMatch(result -> hasKnownQueryLanguage(result.getVersion()));
    if (hasTypedVersion) {
      declareQueryLanguageProperty(model);
    }
  }

  private static void addCountMetricDeclaration(Model model, String localName, String label) {
    Resource metric = model.createResource(NS_FDQF + localName);
    metric.addProperty(RDF.type, model.createResource(NS_DQV + "Metric"));
    metric.addProperty(
        model.createProperty(NS_DQV, "inDimension"), model.createResource(NS_FDQF + "volume"));
    metric.addProperty(
        model.createProperty(NS_DQV, "expectedDataType"),
        model.createResource(XSDDatatype.XSDinteger.getURI()));
    metric.addProperty(
        model.createProperty(NS_SKOS, "prefLabel"), model.createLiteral(label, "en"));
  }

  private static void declareStatusTerms(Model model, List<QualityCheckResult> results) {
    Resource scheme = model.createResource(NS_FDQF + "statuses");
    scheme.addProperty(RDF.type, model.createResource(NS_SKOS + "ConceptScheme"));
    Map<String, String> statusLabels =
        Map.of(
            "passed", "Passed",
            "warning", "Warning",
            "failed", "Failed",
            "notComputed", "Not computed");
    for (Map.Entry<String, String> statusLabel : statusLabels.entrySet()) {
      boolean used =
          results.stream().anyMatch(result -> statusLabel.getKey().equals(statusLocalName(result)));
      if (!used) {
        continue;
      }
      Resource concept = model.createResource(NS_FDQF + statusLabel.getKey());
      concept.addProperty(RDF.type, model.createResource(NS_SKOS + "Concept"));
      concept.addProperty(
          model.createProperty(NS_SKOS, "prefLabel"),
          model.createLiteral(statusLabel.getValue(), "en"));
      concept.addProperty(model.createProperty(NS_SKOS, "inScheme"), scheme);
    }
  }

  private static void declareThresholdProperty(Model model, String localName, String comment) {
    Resource property = model.createResource(NS_FDQF + localName);
    property.addProperty(RDF.type, RDF.Property);
    property.addProperty(RDF.type, model.createResource(NS_OWL + "DatatypeProperty"));
    property.addProperty(RDFS.range, model.createResource(XSDDatatype.XSDdouble.getURI()));
    property.addProperty(RDFS.comment, model.createLiteral(comment, "en"));
  }

  private static void declareQueryLanguageProperty(Model model) {
    Resource property = model.createResource(NS_FDQF + "queryLanguage");
    property.addProperty(RDF.type, RDF.Property);
    property.addProperty(RDF.type, model.createResource(NS_OWL + "DatatypeProperty"));
    property.addProperty(RDFS.range, model.createResource(XSDDatatype.XSDstring.getURI()));
    property.addProperty(RDFS.comment, model.createLiteral(QUERY_LANGUAGE_COMMENT, "en"));
  }

  private static void addChecksAndMetrics(
      Model model, String base, List<QualityCheckResult> results) {
    Map<Long, QualityCheck> checksById = new TreeMap<>();
    Map<Long, List<QualityCheckResult>> resultsByCheckId = new TreeMap<>();
    for (QualityCheckResult result : results) {
      Long checkId = result.getQualityCheck().getId();
      checksById.putIfAbsent(checkId, result.getQualityCheck());
      resultsByCheckId.computeIfAbsent(checkId, key -> new ArrayList<>()).add(result);
    }
    for (Map.Entry<Long, QualityCheck> entry : checksById.entrySet()) {
      addCheck(model, base, entry.getValue());
      for (QualityCheckResult result : resultsByCheckId.get(entry.getKey())) {
        addMetric(model, base, result);
      }
    }
  }

  private static void addCheck(Model model, String base, QualityCheck check) {
    List<String> keywords =
        check.getKeywords().stream()
            .map(QualityCheckKeyword::getKeyword)
            .filter(ReportDqvServiceImpl::notBlank)
            .distinct()
            .sorted()
            .toList();
    if (!notBlank(check.getName()) && keywords.isEmpty()) {
      return;
    }
    Resource checkResource = model.createResource(checkIri(base, check.getId()));
    if (notBlank(check.getName())) {
      checkResource.addProperty(
          model.createProperty(NS_SKOS, "prefLabel"), model.createLiteral(check.getName()));
    }
    if (notBlank(check.getDescription())) {
      checkResource.addProperty(
          model.createProperty(NS_SKOS, "definition"), model.createLiteral(check.getDescription()));
    }
    Property keyword = model.createProperty(NS_DCAT, "keyword");
    for (String keywordValue : keywords) {
      checkResource.addProperty(keyword, model.createLiteral(keywordValue));
    }
  }

  /**
   * Adds a quality check version as a {@code dqv:Metric}. Only version-stable facts are attached:
   * the query hash and language are immutable for a version, whereas name, thresholds and category
   * live on the editable {@link QualityCheck} and are therefore exported elsewhere.
   */
  private static void addMetric(Model model, String base, QualityCheckResult result) {
    QualityCheck check = result.getQualityCheck();
    QualityCheckVersion version = result.getVersion();
    Resource metric = model.createResource(metricIri(base, result));
    metric.addProperty(RDF.type, model.createResource(NS_DQV + "Metric"));
    metric.addProperty(
        model.createProperty(NS_DQV, "expectedDataType"),
        model.createResource(XSDDatatype.XSDdouble.getURI()));
    metric.addProperty(
        model.createProperty(NS_DCTERMS, "isVersionOf"),
        model.createResource(checkIri(base, check.getId())));
    metric.addProperty(
        model.createProperty(NS_OWL, "versionInfo"),
        model.createLiteral(Integer.toString(version.getVersion())));
    metric.addProperty(
        model.createProperty(NS_DCTERMS, "identifier"), model.createLiteral(version.getHash()));
    // Categories change rarely and are needed to group measurements by dimension in DQV.
    metric.addProperty(
        model.createProperty(NS_DQV, "inDimension"), dimensionOf(model, base, check));
    if (hasKnownQueryLanguage(version)) {
      metric.addProperty(
          model.createProperty(NS_FDQF, "queryLanguage"),
          model.createLiteral(version.getType().name()));
    }
    metric.addProperty(RDFS.comment, model.createLiteral(SUPPRESSION_COMMENT, "en"));
  }

  private static boolean hasKnownQueryLanguage(QualityCheckVersion version) {
    return version.getType() != null && version.getType() != QueryType.UNKNOWN;
  }

  private static void addMeasurements(
      Model model, String base, Report report, List<QualityCheckResult> results) {
    if (report.getTotalPatients() != null) {
      addCountMeasurement(
          model, base, report, "patients", "patientCount", report.getTotalPatients());
    }
    if (report.getTotalSamples() != null) {
      addCountMeasurement(model, base, report, "samples", "sampleCount", report.getTotalSamples());
    }
    for (QualityCheckResult result : results) {
      if (!isComputable(result)) {
        continue;
      }
      Resource measurement =
          model.createResource(measurementIri(base, report, resultFragment(result)));
      measurement.addProperty(RDF.type, model.createResource(NS_DQV + "QualityMeasurement"));
      measurement.addProperty(
          model.createProperty(NS_QB, "dataSet"), model.createResource(reportIri(base, report)));
      measurement.addProperty(
          model.createProperty(NS_DQV, "computedOn"),
          model.createResource(datasetIri(base, report.getAgentId())));
      measurement.addProperty(
          model.createProperty(NS_DQV, "isMeasurementOf"),
          model.createResource(metricIri(base, result)));
      measurement.addProperty(
          model.createProperty(NS_DQV, "value"), model.createTypedLiteral(result.getResult()));
      measurement.addProperty(
          model.createProperty(NS_SDMX_ATTRIBUTE, "unitMeasure"),
          model.createResource(NS_OM + "one"));
    }
  }

  private static void addCountMeasurement(
      Model model, String base, Report report, String fragment, String metricLocal, int value) {
    Resource measurement = model.createResource(measurementIri(base, report, fragment));
    measurement.addProperty(RDF.type, model.createResource(NS_DQV + "QualityMeasurement"));
    measurement.addProperty(
        model.createProperty(NS_QB, "dataSet"), model.createResource(reportIri(base, report)));
    measurement.addProperty(
        model.createProperty(NS_DQV, "computedOn"),
        model.createResource(datasetIri(base, report.getAgentId())));
    measurement.addProperty(
        model.createProperty(NS_DQV, "isMeasurementOf"),
        model.createResource(NS_FDQF + metricLocal));
    measurement.addProperty(
        model.createProperty(NS_DQV, "value"),
        model.createTypedLiteral(Integer.toString(value), XSDDatatype.XSDinteger));
    measurement.addProperty(RDFS.comment, model.createLiteral(ROUNDING_COMMENT, "en"));
  }

  /**
   * Adds one {@code dqv:QualityAnnotation} per result. Annotations are a snapshot of the
   * classification at export time: their IRI is export-specific (see {@code exportedAt}), because
   * thresholds, category and status are recomputed on every export, and merging two exports must
   * not attribute conflicting assessments to one annotation. Measurement and metric IRIs remain
   * stable. The dimension is attached to the metric (the domain of {@code dqv:inDimension}); the
   * annotation keeps the applied thresholds and the export time instead.
   */
  private static void addAnnotations(
      Model model,
      String base,
      Report report,
      List<QualityCheckResult> results,
      Instant exportedAt,
      String exportId) {
    for (QualityCheckResult result : results) {
      QualityCheck check = result.getQualityCheck();
      Resource annotation = model.createResource(annotationIri(base, report, result, exportId));
      annotation.addProperty(RDF.type, model.createResource(NS_DQV + "QualityAnnotation"));
      annotation.addProperty(
          model.createProperty(NS_OA, "motivatedBy"),
          model.createResource(NS_DQV + "qualityAssessment"));
      annotation.addProperty(
          model.createProperty(NS_OA, "motivatedBy"), model.createResource(NS_OA + "classifying"));
      annotation.addProperty(
          model.createProperty(NS_OA, "hasTarget"),
          model.createResource(datasetIri(base, report.getAgentId())));
      annotation.addProperty(
          model.createProperty(NS_PROV, "wasAttributedTo"), model.createResource(serverIri(base)));
      annotation.addProperty(
          model.createProperty(NS_PROV, "generatedAtTime"),
          model.createTypedLiteral(exportedAt.toString(), XSDDatatype.XSDdateTime));
      // The thresholds applied at export time are stored on the annotation, not on the metric,
      // because they live on the editable quality check and may change between exports.
      annotation.addProperty(
          model.createProperty(NS_FDQF, "warningThreshold"),
          model.createTypedLiteral(fractionOfPercent(check.getWarningThreshold())));
      annotation.addProperty(
          model.createProperty(NS_FDQF, "errorThreshold"),
          model.createTypedLiteral(fractionOfPercent(check.getErrorThreshold())));
      if (!isComputable(result)) {
        // DQV mandates a value on a measurement, so an uncomputed or non-finite check is expressed
        // as an annotation derived directly from the metric only.
        annotation.addProperty(
            model.createProperty(NS_OA, "hasBody"), model.createResource(NS_FDQF + "notComputed"));
        annotation.addProperty(
            model.createProperty(NS_PROV, "wasDerivedFrom"),
            model.createResource(metricIri(base, result)));
      } else {
        annotation.addProperty(
            model.createProperty(NS_OA, "hasBody"),
            model.createResource(
                NS_FDQF + statusLocalName(CheckStatus.of(check, result.getResult()))));
        annotation.addProperty(
            model.createProperty(NS_PROV, "wasDerivedFrom"),
            model.createResource(measurementIri(base, report, resultFragment(result))));
      }
    }
  }

  private static Resource dimensionOf(Model model, String base, QualityCheck check) {
    Category category = check.getCategory();
    return category != null && category.getId() != null
        ? model.createResource(categoryIri(base, category.getId()))
        : model.createResource(NS_FDQF + UNCATEGORISED_DIMENSION);
  }

  /** Statuses are only exported for computable results; others are reported as not computed. */
  private static String statusLocalName(CheckStatus status) {
    return switch (status) {
      case PASSED -> "passed";
      case WARNING -> "warning";
      case FAILED -> "failed";
    };
  }

  private static boolean isComputable(QualityCheckResult result) {
    Double value = result.getResult();
    return value != null && Double.isFinite(value);
  }

  private static String statusLocalName(QualityCheckResult result) {
    if (!isComputable(result)) {
      return "notComputed";
    }
    return statusLocalName(CheckStatus.of(result.getQualityCheck(), result.getResult()));
  }

  private static List<String> measurementIris(
      String base, Report report, List<QualityCheckResult> results) {
    List<String> iris = new ArrayList<>();
    if (report.getTotalPatients() != null) {
      iris.add(measurementIri(base, report, "patients"));
    }
    if (report.getTotalSamples() != null) {
      iris.add(measurementIri(base, report, "samples"));
    }
    for (QualityCheckResult result : results) {
      if (isComputable(result)) {
        iris.add(measurementIri(base, report, resultFragment(result)));
      }
    }
    return iris;
  }

  private static List<String> partIris(
      String base, Report report, List<QualityCheckResult> results, String exportId) {
    List<String> parts = new ArrayList<>(measurementIris(base, report, results));
    for (QualityCheckResult result : results) {
      parts.add(annotationIri(base, report, result, exportId));
    }
    return parts;
  }

  private static List<QualityCheckResult> sortedResults(Report report) {
    return report.getQualityCheckResults().stream()
        .sorted(
            Comparator.comparing((QualityCheckResult r) -> r.getQualityCheck().getId())
                .thenComparingInt(r -> r.getVersion().getVersion()))
        .toList();
  }

  private static String reportIri(String base, Report report) {
    return base + "reports/" + encodeSegment(report.getId());
  }

  private static String agentIri(String base, String agentId) {
    return base + "agents/" + encodeSegment(agentId) + "#agent";
  }

  private static String datasetIri(String base, String agentId) {
    return base + "agents/" + encodeSegment(agentId) + "#dataset";
  }

  private static String serverIri(String base) {
    return base + "#server";
  }

  private static String checkIri(String base, Long checkId) {
    return base + "quality-checks/" + checkId;
  }

  private static String metricIri(String base, QualityCheckResult result) {
    return checkIri(base, result.getQualityCheck().getId())
        + "/versions/"
        + result.getVersion().getVersion();
  }

  private static String categoriesIri(String base) {
    return base + "categories";
  }

  private static String categoryIri(String base, Long categoryId) {
    return base + "categories/" + categoryId;
  }

  private static String measurementIri(String base, Report report, String fragment) {
    return reportIri(base, report) + "#m-" + fragment;
  }

  private static String annotationIri(
      String base, Report report, QualityCheckResult result, String exportId) {
    return reportIri(base, report) + "#s-" + resultFragment(result) + "-e" + exportId;
  }

  /** Converts a percentage threshold (e.g. 0.7) into an exact-decimal ratio (e.g. 0.007). */
  private static double fractionOfPercent(double percent) {
    // Decimal scaling avoids binary division artifacts (0.7 / 100 is 0.006999999999999999);
    // non-finite thresholds keep their canonical xsd:double lexical forms (NaN, INF, -INF).
    return Double.isFinite(percent)
        ? BigDecimal.valueOf(percent).movePointLeft(2).doubleValue()
        : percent / 100.0;
  }

  private static String resultFragment(QualityCheckResult result) {
    return result.getQualityCheck().getId() + "-v" + result.getVersion().getVersion();
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  /** Percent-encodes all characters outside the RFC 3986 unreserved set within an IRI segment. */
  private static String encodeSegment(String segment) {
    StringBuilder encoded = new StringBuilder(segment.length());
    for (byte b : segment.getBytes(StandardCharsets.UTF_8)) {
      int c = b & 0xFF;
      if (isUnreserved(c)) {
        encoded.append((char) c);
      } else {
        encoded.append("%%%02X".formatted(c));
      }
    }
    return encoded.toString();
  }

  private static boolean isUnreserved(int c) {
    return (c >= 'A' && c <= 'Z')
        || (c >= 'a' && c <= 'z')
        || (c >= '0' && c <= '9')
        || c == '-'
        || c == '.'
        || c == '_'
        || c == '~';
  }
}
