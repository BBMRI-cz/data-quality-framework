package eu.bbmri_eric.quality.agent.fhir;

import org.json.JSONObject;

public interface FHIRStore {
  public JSONObject libraryTemplate();

  public JSONObject measureTemplate();

  public JSONObject createLibrary(String libraryUri, String cqlData);

  public JSONObject createMeasure(String measureUri, String libraryUri, String subjectType);

  JSONObject postResource(String resourceType, JSONObject resource);

  JSONObject evaluateMeasure(String measureId);

  JSONObject evaluateMeasureList(String baseUrl, String measureId);

  int countResources(String resourceType);
}
