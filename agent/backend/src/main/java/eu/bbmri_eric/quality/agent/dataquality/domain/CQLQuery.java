package eu.bbmri_eric.quality.agent.dataquality.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.FHIRStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

/** A data quality check utilizing the Hl7 Clinical Quality Language queries for evaluation. */
@Entity(name = "cql_check")
@Getter
@Setter
public class CQLQuery implements DataQualityCheck {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String name;
  @NotBlank private String description;
  @NotNull private String query;
  private int warningThreshold = 10;
  private int errorThreshold = 30;
  private float epsilonBudget = 1.0f;

  protected CQLQuery() {}

  public CQLQuery(
      Long id, @NotNull String name, @NotNull String description, @NotNull String query) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.query = query;
  }

  public CQLQuery(String name, String description, String query) {
    this.name = name;
    this.description = description;
    this.query = query;
  }

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public ResultDTO execute(FHIRStore fhirStore) {
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
