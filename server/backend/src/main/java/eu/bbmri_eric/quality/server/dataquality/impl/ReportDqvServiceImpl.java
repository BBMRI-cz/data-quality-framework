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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Service implementation for exporting reports in the W3C Data Quality Vocabulary (DQV).
 *
 * <p>A report is rendered as a {@code dqv:QualityMeasurementDataset}. Each valued check result
 * becomes a {@code dqv:QualityMeasurement}, and every result is additionally classified with a
 * {@code dqv:QualityAnnotation} derived from the check thresholds (same rule as the PDF summary).
 * Query texts are never serialized; only the query hash and language are exposed on the metric.
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
      "Values are differentially private estimates computed at the data source.";
  private static final String ROUNDING_COMMENT = "Rounded up to the nearest 10.";
  private static final String UNCATEGORISED_DIMENSION = "uncategorised";

  private final ReportRepository reportRepository;
  private final AgentRepository agentRepository;

  public ReportDqvServiceImpl(ReportRepository reportRepository, AgentRepository agentRepository) {
    this.reportRepository = reportRepository;
    this.agentRepository = agentRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public String exportDqv(String reportId) {
    Report report =
        reportRepository
            .findById(reportId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException("Report with ID %s not found".formatted(reportId)));
    Agent agent = agentRepository.findById(report.getAgentId()).orElse(null);
    String base =
        ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/").build().toUriString();
    logger.debug("Exporting report {} as DQV (base IRI: {})", reportId, base);

    Model model = ModelFactory.createDefaultModel();
    registerPrefixes(model);
    List<QualityCheckResult> results = sortedResults(report);
    addReport(model, base, report, results);
    addAgent(model, base, report, agent, results);
    addCategories(model, base, results);
    addVocabularyTerms(model, report, results);
    addChecksAndMetrics(model, base, results);
    addMeasurements(model, base, report, results);
    addAnnotations(model, base, report, results);
    return RDFWriter.source(model).lang(Lang.TURTLE).base(base).asString();
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
      Model model, String base, Report report, List<QualityCheckResult> results) {
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
    for (String part : partIris(base, report, results)) {
      reportResource.addProperty(
          model.createProperty(NS_DCTERMS, "hasPart"), model.createResource(part));
    }
  }

  private static void addAgent(
      Model model, String base, Report report, Agent agent, List<QualityCheckResult> results) {
    String label =
        agent != null && notBlank(agent.getName()) ? agent.getName() : report.getAgentId();
    Resource agentResource = model.createResource(agentIri(base, report.getAgentId()));
    agentResource.addProperty(RDF.type, model.createResource(NS_PROV + "SoftwareAgent"));
    agentResource.addProperty(RDFS.label, model.createLiteral(label, "en"));
    if (agent != null && notBlank(agent.getVersion())) {
      agentResource.addProperty(
          model.createProperty(NS_OWL, "versionInfo"), model.createLiteral(agent.getVersion()));
    }
    Resource datasetResource = model.createResource(datasetIri(base, report.getAgentId()));
    datasetResource.addProperty(RDF.type, model.createResource(NS_DCAT + "Dataset"));
    datasetResource.addProperty(
        model.createProperty(NS_DCTERMS, "title"), model.createLiteral(label, "en"));
    Property hasQualityMeasurement = model.createProperty(NS_DQV, "hasQualityMeasurement");
    for (String measurementIri : measurementIris(base, report, results)) {
      datasetResource.addProperty(hasQualityMeasurement, model.createResource(measurementIri));
    }
    Property hasQualityAnnotation = model.createProperty(NS_DQV, "hasQualityAnnotation");
    for (QualityCheckResult result : results) {
      datasetResource.addProperty(
          hasQualityAnnotation,
          model.createResource(annotationIri(base, report, resultFragment(result))));
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
            model.createProperty(NS_SKOS, "prefLabel"),
            model.createLiteral(category.getName(), "en"));
      }
      categoryResource.addProperty(
          model.createProperty(NS_SKOS, "inScheme"), model.createResource(categoriesIri(base)));
    }
  }

  /**
   * Declares the FDQF vocabulary terms used by the document. Dimensions and metrics are declared
   * explicitly because DQV requires every {@code dqv:Metric} to be in at least one {@code
   * dqv:Dimension}.
   */
  private static void addVocabularyTerms(
      Model model, Report report, List<QualityCheckResult> results) {
    if (report.getTotalPatients() != null || report.getTotalSamples() != null) {
      model
          .createResource(NS_FDQF + "volume")
          .addProperty(RDF.type, model.createResource(NS_DQV + "Dimension"));
    }
    if (report.getTotalPatients() != null) {
      addCountMetric(model, "patientCount");
    }
    if (report.getTotalSamples() != null) {
      addCountMetric(model, "sampleCount");
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
  }

  private static void addCountMetric(Model model, String localName) {
    Resource metric = model.createResource(NS_FDQF + localName);
    metric.addProperty(RDF.type, model.createResource(NS_DQV + "Metric"));
    metric.addProperty(
        model.createProperty(NS_DQV, "inDimension"), model.createResource(NS_FDQF + "volume"));
  }

  private static void addChecksAndMetrics(
      Model model, String base, List<QualityCheckResult> results) {
    Map<Long, QualityCheck> checksById = new TreeMap<>();
    for (QualityCheckResult result : results) {
      checksById.putIfAbsent(result.getQualityCheck().getId(), result.getQualityCheck());
    }
    for (QualityCheck check : checksById.values()) {
      List<String> keywords =
          check.getKeywords().stream()
              .map(QualityCheckKeyword::getKeyword)
              .filter(ReportDqvServiceImpl::notBlank)
              .distinct()
              .sorted()
              .toList();
      if (!keywords.isEmpty()) {
        Resource checkResource = model.createResource(checkIri(base, check.getId()));
        Property keyword = model.createProperty(NS_DCAT, "keyword");
        for (String keywordValue : keywords) {
          checkResource.addProperty(keyword, model.createLiteral(keywordValue));
        }
      }
      for (QualityCheckResult result : results) {
        if (result.getQualityCheck().getId().equals(check.getId())) {
          addMetric(model, base, result);
        }
      }
    }
  }

  private static void addMetric(Model model, String base, QualityCheckResult result) {
    QualityCheck check = result.getQualityCheck();
    QualityCheckVersion version = result.getVersion();
    Resource metric = model.createResource(metricIri(base, result));
    metric.addProperty(RDF.type, model.createResource(NS_DQV + "Metric"));
    if (notBlank(check.getName())) {
      metric.addProperty(
          model.createProperty(NS_SKOS, "prefLabel"), model.createLiteral(check.getName(), "en"));
    }
    if (notBlank(check.getDescription())) {
      metric.addProperty(
          model.createProperty(NS_SKOS, "definition"),
          model.createLiteral(check.getDescription(), "en"));
    }
    metric.addProperty(
        model.createProperty(NS_DQV, "expectedDataType"),
        model.createResource(XSDDatatype.XSDdouble.getURI()));
    Category category = check.getCategory();
    metric.addProperty(
        model.createProperty(NS_DQV, "inDimension"),
        category != null && category.getId() != null
            ? model.createResource(categoryIri(base, category.getId()))
            : model.createResource(NS_FDQF + UNCATEGORISED_DIMENSION));
    metric.addProperty(
        model.createProperty(NS_DCTERMS, "isVersionOf"),
        model.createResource(checkIri(base, check.getId())));
    metric.addProperty(
        model.createProperty(NS_OWL, "versionInfo"),
        model.createLiteral(Integer.toString(version.getVersion())));
    metric.addProperty(
        model.createProperty(NS_DCTERMS, "identifier"), model.createLiteral(version.getHash()));
    if (version.getType() != null && version.getType() != QueryType.UNKNOWN) {
      metric.addProperty(
          model.createProperty(NS_FDQF, "queryLanguage"),
          model.createLiteral(version.getType().name()));
    }
    metric.addProperty(
        model.createProperty(NS_FDQF, "warningThreshold"),
        model.createTypedLiteral(
            percentToFraction(check.getWarningThreshold()), XSDDatatype.XSDdouble));
    metric.addProperty(
        model.createProperty(NS_FDQF, "errorThreshold"),
        model.createTypedLiteral(
            percentToFraction(check.getErrorThreshold()), XSDDatatype.XSDdouble));
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
      if (result.getResult() == null) {
        continue;
      }
      String fragment = resultFragment(result);
      Resource measurement = model.createResource(measurementIri(base, report, fragment));
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
          model.createProperty(NS_DQV, "value"),
          model.createTypedLiteral(formatDouble(result.getResult()), XSDDatatype.XSDdouble));
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

  private static void addAnnotations(
      Model model, String base, Report report, List<QualityCheckResult> results) {
    for (QualityCheckResult result : results) {
      String fragment = resultFragment(result);
      Resource annotation = model.createResource(annotationIri(base, report, fragment));
      annotation.addProperty(RDF.type, model.createResource(NS_DQV + "QualityAnnotation"));
      annotation.addProperty(
          model.createProperty(NS_OA, "motivatedBy"),
          model.createResource(NS_DQV + "qualityAssessment"));
      annotation.addProperty(
          model.createProperty(NS_OA, "motivatedBy"), model.createResource(NS_OA + "classifying"));
      annotation.addProperty(
          model.createProperty(NS_OA, "hasTarget"),
          model.createResource(datasetIri(base, report.getAgentId())));
      if (result.getResult() == null) {
        // DQV mandates a value on a measurement, so an uncomputed check is expressed as an
        // annotation derived directly from the metric only.
        annotation.addProperty(
            model.createProperty(NS_OA, "hasBody"), model.createResource(NS_FDQF + "notComputed"));
        annotation.addProperty(
            model.createProperty(NS_PROV, "wasDerivedFrom"),
            model.createResource(metricIri(base, result)));
      } else {
        annotation.addProperty(
            model.createProperty(NS_OA, "hasBody"),
            model.createResource(NS_FDQF + statusLocalName(result)));
        annotation.addProperty(
            model.createProperty(NS_PROV, "wasDerivedFrom"),
            model.createResource(measurementIri(base, report, fragment)));
      }
    }
  }

  private static String statusLocalName(QualityCheckResult result) {
    // Mirrors the PASSED/WARNING/FAILED rule used by the PDF summary.
    double percentage = result.getResult() * 100.0;
    QualityCheck check = result.getQualityCheck();
    if (percentage > check.getErrorThreshold()) {
      return "failed";
    }
    if (percentage > check.getWarningThreshold()) {
      return "warning";
    }
    return "passed";
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
      if (result.getResult() != null) {
        iris.add(measurementIri(base, report, resultFragment(result)));
      }
    }
    return iris;
  }

  private static List<String> partIris(
      String base, Report report, List<QualityCheckResult> results) {
    List<String> parts = new ArrayList<>(measurementIris(base, report, results));
    for (QualityCheckResult result : results) {
      parts.add(annotationIri(base, report, resultFragment(result)));
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

  private static String checkIri(String base, Long checkId) {
    return base + "quality-checks/" + checkId;
  }

  private static String categoriesIri(String base) {
    return base + "categories";
  }

  private static String categoryIri(String base, Long categoryId) {
    return base + "categories/" + categoryId;
  }

  private static String metricIri(String base, QualityCheckResult result) {
    return checkIri(base, result.getQualityCheck().getId())
        + "/versions/"
        + result.getVersion().getVersion();
  }

  private static String measurementIri(String base, Report report, String fragment) {
    return reportIri(base, report) + "#m-" + fragment;
  }

  private static String annotationIri(String base, Report report, String fragment) {
    return reportIri(base, report) + "#s-" + fragment;
  }

  private static String resultFragment(QualityCheckResult result) {
    return result.getQualityCheck().getId() + "-v" + result.getVersion().getVersion();
  }

  /** Formats a ratio (0–1) in canonical {@code xsd:double} lexical form. */
  private static String formatDouble(double value) {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  /** Converts a percentage threshold (e.g. 5.0) into a ratio (e.g. 0.05). */
  private static String percentToFraction(double percent) {
    return BigDecimal.valueOf(percent).movePointLeft(2).stripTrailingZeros().toPlainString();
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
