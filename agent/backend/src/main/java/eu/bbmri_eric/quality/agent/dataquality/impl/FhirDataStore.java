package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import java.util.NoSuchElementException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
class FhirDataStore implements DataStore {
  private final FHIRServer fhirStore;

  public FhirDataStore(FHIRServer fhirServer) {
    this.fhirStore = fhirServer;
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

  @Override
  public Object executeQuery(String query) {
    return null;
  }

  @Override
  public DatabaseHealthDTO checkHealthV2() {
    return null;
  }
}
