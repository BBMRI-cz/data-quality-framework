package eu.bbmri_eric.quality.agent.dataquality.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

public final class FhirCqlQueryExecutor {
  private static final ObjectMapper mapper = new ObjectMapper();

  private FhirCqlQueryExecutor() {}

  public static ResultDTO execute(FHIRServer fhirStore, String query) {
    try {
      String cqlData = Base64.getEncoder().encodeToString(query.getBytes());
      String libraryUri = java.util.UUID.randomUUID().toString().toLowerCase();
      String measureUri = java.util.UUID.randomUUID().toString().toLowerCase();
      JSONObject libraryResource = fhirStore.createLibrary(libraryUri, cqlData);
      fhirStore.postResource("Library", libraryResource);
      JSONObject measureResource = fhirStore.createMeasure(measureUri, libraryUri, "Patient");
      JSONObject measureResponse = fhirStore.postResource("Measure", measureResource);
      String measureId = measureResponse.getString("id");
      JSONObject measureReport = fhirStore.evaluateMeasureList(measureId);
      JsonNode mr = mapper.readTree(measureReport.toString());

      int count = mr.at("/group/0/population/0/count").asInt();
      Set<String> idSet = new HashSet<>();
      if (count != 0) {
        String listRef = mr.at("/group/0/population/0/subjectResults/reference").asText(null);
        if (listRef != null && listRef.startsWith("List/")) {
          String listId = listRef.substring("List/".length());

          JSONObject listResource = fhirStore.getPatientList(listId);
          JsonNode lr = mapper.readTree(listResource.toString());

          for (JsonNode entry : lr.withArray("entry")) {
            String ref = entry.at("/item/reference").asText(null);
            if (ref != null && ref.startsWith("Patient/")) {
              idSet.add(ref.substring("Patient/".length()));
            }
          }
        }
      }

      return new ResultDTO(count, "Patient", idSet);
    } catch (Exception | NoSuchMethodError e) {
      return new ResultDTO(e.getMessage());
    }
  }
}
