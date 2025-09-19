package eu.bbmri_eric.quality.agent.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

/** A data quality check utilizing the Hl7 Clinical Quality Language queries for evaluation. */
@Entity(name = "cql_check")
class CQLQuery implements DataQualityCheck {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String name;
  @NotBlank private String description;

  @NotNull @Lob private String query;

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
  public Result execute(FHIRStore fhirStore) {
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

      return new Result(count, "Patient", idSet);
    } catch (Exception | NoSuchMethodError e) {
      return new Result(e.getMessage());
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public int getWarningThreshold() {
    return warningThreshold;
  }

  public void setWarningThreshold(int warningThreshold) {
    this.warningThreshold = warningThreshold;
  }

  public int getErrorThreshold() {
    return errorThreshold;
  }

  public void setErrorThreshold(int errorThreshold) {
    this.errorThreshold = errorThreshold;
  }

  public float getEpsilonBudget() {
    return epsilonBudget;
  }

  public void setEpsilonBudget(float epsilonBudget) {
    this.epsilonBudget = epsilonBudget;
  }
}
