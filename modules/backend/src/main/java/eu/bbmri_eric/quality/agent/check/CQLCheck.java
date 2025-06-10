package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity(name = "cql_check")
public class CQLCheck implements Check {
  private static final Logger log = LoggerFactory.getLogger(CQLCheck.class);
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String description;
  private String query;
  private int warningThreshold = 10;
  private int errorThreshold = 30;
  private float epsilonBudget = 1.0f;

  protected CQLCheck() {}

  public CQLCheck(Long id, String name, String description, String query) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.query = query;
  }

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
      JSONObject measureReport = fhirStore.evaluateMeasure(measureId);
      int count =
          measureReport
              .getJSONArray("group")
              .optJSONObject(0, new JSONObject())
              .getJSONArray("population")
              .optJSONObject(0, new JSONObject())
              .optInt("count", 0);
      return new Result(count, "Patient");
    } catch (Exception | NoSuchMethodError e) {
      log.error("Check {} failed {}", id ,e.getMessage());
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
