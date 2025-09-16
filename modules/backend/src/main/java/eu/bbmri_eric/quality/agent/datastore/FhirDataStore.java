package eu.bbmri_eric.quality.agent.datastore;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.util.NoSuchElementException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class FhirDataStore implements DataStore {
  private final FHIRStore fhirStore;

  public FhirDataStore(FHIRStore fhirStore) {
    this.fhirStore = fhirStore;
  }

  @Override
  public JSONObject getEntity(String entityType, String id) {
    if (entityType.equals("Patient")) {
      JSONObject patient = fhirStore.getPatientEverything(id);
      if (patient == null) {
        throw new NoSuchElementException("Patient with ID " + id + " not found.");
      }
      return patient;
    }
    throw new IllegalArgumentException("Unsupported entity type: " + entityType);
  }

  @Override
  public JSONObject checkHealth() {
    return fhirStore.checkHealth();
  }
}
