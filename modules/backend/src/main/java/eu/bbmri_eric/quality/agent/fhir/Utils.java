package eu.bbmri_eric.quality.agent.fhir;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class Utils {

  public static JSONObject libraryTemplate() {
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

  public static JSONObject measureTemplate() {
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
    measure.put("group", group);

    return measure;
  }

  public static JSONObject createLibrary(String libraryUri, String cqlData) {
    JSONObject library = libraryTemplate();
    library.put("url", "urn:uuid:" + libraryUri);
    library.getJSONArray("content").getJSONObject(0).put("data", cqlData);
    return library;
  }

  public static JSONObject createMeasure(String measureUri, String libraryUri, String subjectType) {
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

  public static JSONObject postResource(String baseUrl, String resourceType, JSONObject resource) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(resource.toString(), headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              baseUrl + "/" + resourceType, HttpMethod.POST, entity, String.class);
      return new JSONObject(response.getBody());
    } catch (HttpClientErrorException e) {
      throw new RuntimeException("HTTP error: " + e.getStatusCode(), e);
    }
  }

  public static JSONObject evaluateMeasure(String baseUrl, String measureId) {
    RestTemplate restTemplate = new RestTemplate();
    String url =
        baseUrl + "/Measure/" + measureId + "/$evaluate-measure?periodStart=2000&periodEnd=2030";
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      return new JSONObject(response.getBody());
    } catch (HttpClientErrorException e) {
      throw new RuntimeException("HTTP error: " + e.getStatusCode(), e);
    }
  }

  public static JSONObject evaluateMeasureList(String baseUrl, String measureId) {
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
}
