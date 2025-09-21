package eu.bbmri_eric.quality.agent.datastore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

class DataStoreTest {

  @Test
  void getEntity_success() throws Exception {
    DataStore mockDataStore = Mockito.mock(DataStore.class);
    JSONObject entity = new JSONObject().put("id", "123").put("type", "Patient");
    Mockito.when(mockDataStore.getEntity("Patient", "123")).thenReturn(entity);

    EntityController controller = new EntityController(mockDataStore);
    ResponseEntity<String> response = controller.getEntity("Patient", "123");

    assertEquals(200, response.getStatusCodeValue());
    assertTrue(response.getBody().contains("\"id\":\"123\""));
    assertTrue(response.getBody().contains("\"type\":\"Patient\""));
  }

  @Test
  void getEntity_error() throws Exception {
    DataStore mockDataStore = Mockito.mock(DataStore.class);
    Mockito.when(mockDataStore.getEntity("Patient", "fail"))
        .thenThrow(new RuntimeException("Not found"));

    EntityController controller = new EntityController(mockDataStore);
    ResponseEntity<String> response = controller.getEntity("Patient", "fail");

    assertEquals(500, response.getStatusCodeValue());
    assertTrue(response.getBody().contains("Failed to retrieve entity"));
  }
}
