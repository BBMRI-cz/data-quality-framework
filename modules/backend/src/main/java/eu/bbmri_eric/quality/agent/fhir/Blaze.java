package eu.bbmri_eric.quality.agent.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import eu.bbmri_eric.quality.agent.common.ApplicationProperties;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class Blaze implements FHIRStore {
  private final ApplicationProperties applicationProperties;
  private static final Logger log = LoggerFactory.getLogger(Blaze.class);
  private final IGenericClient client;

  public Blaze(ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
    FhirContext ctx = FhirContext.forR4().setParserErrorHandler(new LenientErrorHandler().setErrorOnInvalidValue(false));
    ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    client = ctx.newRestfulGenericClient(applicationProperties.getBaseFHIRUrl());
  }

  public JSONObject libraryTemplate() {
    JSONObject library = new JSONObject();
    library.put("resourceType", "Library");
    library.put("status", "active");

    JSONObject type = new JSONObject();
    JSONArray coding = new JSONArray();
    JSONObject codingEntry = new JSONObject();
    codingEntry.put("system", "http://terminology.hl7.org/CodeSystem/library-type");
    codingEntry.put("code", "logic-library");
    coding.put(codingEntry);
    type.put("coding", coding);
    library.put("type", type);

    JSONArray content = new JSONArray();
    JSONObject contentEntry = new JSONObject();
    contentEntry.put("contentType", "text/cql");
    content.put(contentEntry);
    library.put("content", content);

    return library;
  }

  public JSONObject measureTemplate() {
    JSONObject measure = new JSONObject();
    measure.put("resourceType", "Measure");
    measure.put("status", "active");

    JSONObject subjectCodeableConcept = new JSONObject();
    JSONArray subjectCoding = new JSONArray();
    JSONObject subjectCodingEntry = new JSONObject();
    subjectCodingEntry.put("system", "http://hl7.org/fhir/resource-types");
    subjectCodingEntry.put("code", "Patient");
    subjectCoding.put(subjectCodingEntry);
    subjectCodeableConcept.put("coding", subjectCoding);
    measure.put("subjectCodeableConcept", subjectCodeableConcept);

    JSONObject scoring = new JSONObject();
    JSONArray scoringCoding = new JSONArray();
    JSONObject scoringCodingEntry = new JSONObject();
    scoringCodingEntry.put("system", "http://terminology.hl7.org/CodeSystem/measure-scoring");
    scoringCodingEntry.put("code", "cohort");
    scoringCoding.put(scoringCodingEntry);
    scoring.put("coding", scoringCoding);
    measure.put("scoring", scoring);

    JSONArray group = getJsonArray();
    measure.put("group", group);

    return measure;
  }

  private static JSONArray getJsonArray() {
    JSONArray group = new JSONArray();
    JSONObject groupEntry = new JSONObject();
    JSONArray population = new JSONArray();
    JSONObject populationEntry = new JSONObject();
    JSONObject populationCode = new JSONObject();
    JSONArray populationCoding = new JSONArray();
    JSONObject populationCodingEntry = new JSONObject();
    populationCodingEntry.put("system", "http://terminology.hl7.org/CodeSystem/measure-population");
    populationCodingEntry.put("code", "initial-population");
    populationCoding.put(populationCodingEntry);
    populationCode.put("coding", populationCoding);
    populationEntry.put("code", populationCode);
    JSONObject criteria = new JSONObject();
    criteria.put("language", "text/cql-identifier");
    criteria.put("expression", "InInitialPopulation");
    populationEntry.put("criteria", criteria);
    population.put(populationEntry);
    groupEntry.put("population", population);
    group.put(groupEntry);
    return group;
  }

  public JSONObject createLibrary(String libraryUri, String cqlData) {
    JSONObject library = libraryTemplate();
    library.put("url", "urn:uuid:" + libraryUri);
    library.getJSONArray("content").getJSONObject(0).put("data", cqlData);
    return library;
  }

  public JSONObject createMeasure(String measureUri, String libraryUri, String subjectType) {
    JSONObject measure = measureTemplate();
    measure.put("url", "urn:uuid:" + measureUri);
    JSONArray libraryArray = new JSONArray();
    libraryArray.put("urn:uuid:" + libraryUri);
    measure.put("library", libraryArray);
    measure
        .getJSONObject("subjectCodeableConcept")
        .getJSONArray("coding")
        .getJSONObject(0)
        .put("code", subjectType);
    return measure;
  }

  public JSONObject postResource(String resourceType, JSONObject resource) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(resource.toString(), headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              applicationProperties.getBaseFHIRUrl() + "/" + resourceType,
              HttpMethod.POST,
              entity,
              String.class);
      return new JSONObject(response.getBody());
    } catch (HttpClientErrorException e) {
      throw new RuntimeException("HTTP error: " + e.getStatusCode(), e);
    }
  }

  public JSONObject evaluateMeasure(String measureId) {
    RestTemplate restTemplate = new RestTemplate();
    String url =
        applicationProperties.getBaseFHIRUrl()
            + "/Measure/"
            + measureId
            + "/$evaluate-measure?periodStart=2000&periodEnd=2030";
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      return new JSONObject(response.getBody());
    } catch (HttpClientErrorException e) {
      throw new RuntimeException("HTTP error: " + e.getStatusCode(), e);
    }
  }

  public JSONObject evaluateMeasureList(String baseUrl, String measureId) {
    RestTemplate restTemplate = new RestTemplate();
    JSONObject payload = new JSONObject();
    payload.put("resourceType", "Parameters");
    JSONArray parameters = new JSONArray();
    JSONObject param1 = new JSONObject();
    param1.put("name", "periodStart");
    param1.put("valueDate", "2000");
    JSONObject param2 = new JSONObject();
    param2.put("name", "periodEnd");
    param2.put("valueDate", "2030");
    JSONObject param3 = new JSONObject();
    param3.put("name", "reportType");
    param3.put("valueCode", "subject-list");
    parameters.put(param1);
    parameters.put(param2);
    parameters.put(param3);
    payload.put("parameter", parameters);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              baseUrl + "/Measure/" + measureId + "/$evaluate-measure",
              HttpMethod.POST,
              entity,
              String.class);
      return new JSONObject(response.getBody());
    } catch (HttpClientErrorException e) {
      throw new RuntimeException("HTTP error: " + e.getStatusCode(), e);
    }
  }

  @Override
  public int countResources(String resourceType) {
    try {
      Bundle bundle =
          client
              .search()
              .forResource(resourceType)
              .summaryMode(SummaryEnum.COUNT)
              .returnBundle(Bundle.class)
              .execute();

      return bundle.getTotal(); // no parsing of entries happens
    } catch (Exception e) {
      log.error("Failed to count resources of type: {}", resourceType, e);
      return -1; // Or throw a custom exception if preferred
    }
  }

  @Override
  public List<Resource> fetchAllResources(String resourceType, List<String> elements) {
    List<Resource> resources = new ArrayList<>();
    try {
      // Build the search query with _elements parameter
      Bundle bundle = client
              .search()
              .forResource(resourceType)
              .withAdditionalHeader("Prefer", "handling=lenient")// Request lenient validation
              .returnBundle(Bundle.class)
              .execute();

      // Process the initial bundle
      while (bundle != null) {
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
          Resource resource = entry.getResource();
          if (resource != null) {
            resources.add(resource);
          }
        }

        // Check for next page
        if (bundle.getLink(Bundle.LINK_NEXT) != null) {
          bundle = client
                  .loadPage()
                  .next(bundle)
                  .execute();
        } else {
          bundle = null;
        }
      }
      return resources;
    } catch (Exception e) {
      throw new RuntimeException("Error fetching resources of type " + resourceType + ": " + e.getMessage(), e);
    }
  }
}
