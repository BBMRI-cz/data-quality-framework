package eu.bbmri_eric.quality.agent.datastore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.util.NoSuchElementException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class FhirDataStoreTest {

  @Test
  void returnsEntity_whenPatientFound() throws JSONException {
    FHIRStore fhirStore = mock(FHIRStore.class);
    FhirDataStore store = new FhirDataStore(fhirStore);

    String id = "123";
    JSONObject payload = null;
    try {
      payload = new JSONObject().put("id", id);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    when(fhirStore.getPatientEverything(id)).thenReturn(payload);

    JSONObject result = store.getEntity("Patient", id);

    assertNotNull(result);
    assertEquals(id, result.getString("id"));
    verify(fhirStore, times(1)).getPatientEverything(id);
    verifyNoMoreInteractions(fhirStore);
  }

  @Test
  void throwsNotFound_whenPatientMissing() {
    FHIRStore fhirStore = mock(FHIRStore.class);
    FhirDataStore store = new FhirDataStore(fhirStore);

    String id = "missing-1";
    when(fhirStore.getPatientEverything(id)).thenReturn(null);

    NoSuchElementException ex =
        assertThrows(NoSuchElementException.class, () -> store.getEntity("Patient", id));
    assertTrue(ex.getMessage().contains(id));
    verify(fhirStore, times(1)).getPatientEverything(id);
    verifyNoMoreInteractions(fhirStore);
  }

  @Test
  void throwsBadRequest_onUnsupportedType() {
    FHIRStore fhirStore = mock(FHIRStore.class);
    FhirDataStore store = new FhirDataStore(fhirStore);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> store.getEntity("Observation", "123"));
    assertTrue(ex.getMessage().contains("Unsupported entity type"));
    verifyNoInteractions(fhirStore);
  }
}
