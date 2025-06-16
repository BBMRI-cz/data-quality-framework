package eu.bbmri_eric.quality.agent.fhir;

import java.util.List;
import org.hl7.fhir.r4.model.Resource;
import org.json.JSONObject;

/**
 * Represents an abstraction over a FHIR data store capable of creating, posting, and evaluating
 * FHIR resources such as Libraries and Measures. It provides methods to interact with and query the
 * FHIR backend.
 */
public interface FHIRStore {

  /**
   * Returns a base JSON template for creating a FHIR Library resource.
   *
   * @return a JSON object representing the library template
   */
  JSONObject libraryTemplate();

  /**
   * Returns a base JSON template for creating a FHIR Measure resource.
   *
   * @return a JSON object representing the measure template
   */
  JSONObject measureTemplate();

  /**
   * Creates and uploads a FHIR Library resource to the store.
   *
   * @param libraryUri the canonical URI for the Library
   * @param cqlData the CQL content to include in the Library
   * @return the JSON response from the FHIR server
   */
  JSONObject createLibrary(String libraryUri, String cqlData);

  /**
   * Creates and uploads a FHIR Measure resource to the store.
   *
   * @param measureUri the canonical URI for the Measure
   * @param libraryUri the URI of the Library this Measure depends on
   * @param subjectType the type of FHIR subject (e.g., "Patient") the Measure applies to
   * @return the JSON response from the FHIR server
   */
  JSONObject createMeasure(String measureUri, String libraryUri, String subjectType);

  /**
   * Posts a generic FHIR resource to the store.
   *
   * @param resourceType the type of the FHIR resource (e.g., "Patient", "Observation")
   * @param resource the resource content as a JSON object
   * @return the JSON response from the FHIR server
   */
  JSONObject postResource(String resourceType, JSONObject resource);

  /**
   * Evaluates a specific Measure by its ID.
   *
   * @param measureId the logical ID of the Measure to evaluate
   * @return the JSON result of the Measure evaluation
   */
  JSONObject evaluateMeasure(String measureId);

  /**
   * Evaluates a Measure using a specific base URL context (e.g., subject list).
   *
   * @param baseUrl the base URL for the evaluation context (e.g., a List resource)
   * @param measureId the ID of the Measure to evaluate
   * @return the JSON result of the Measure evaluation
   */
  JSONObject evaluateMeasureList(String baseUrl, String measureId);

  /**
   * Counts the total number of resources of the given type in the store.
   *
   * @param resourceType the type of FHIR resource to count
   * @return the total number of resources found
   */
  int countResources(String resourceType);

  /**
   * Fetches all resources of a given type from the store with optional field selection.
   *
   * @param resourceType the type of FHIR resource to fetch
   * @param elements a list of element names to include in the response (can be empty for full
   *     resources)
   * @return a list of FHIR resources matching the query
   */
  List<Resource> fetchAllResources(String resourceType, List<String> elements);
}
