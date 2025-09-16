package eu.bbmri_eric.quality.agent.datastore;

import java.util.NoSuchElementException;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entities")
public class EntityController {
  private final DataStore dataStore;

  EntityController(DataStore dataStore) {
    this.dataStore = dataStore;
  }

  @GetMapping("{entityType}/{id}")
  public ResponseEntity<String> getEntity(
      @PathVariable String entityType, @PathVariable String id) {
    try {
      JSONObject response = dataStore.getEntity(entityType, id);
      if (response == null) {
        return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Not found.");
      }
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toString());
    } catch (NoSuchElementException e) {
      return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Entity not found.");
    } catch (Exception e) {
      return ResponseEntity.status(500)
          .contentType(MediaType.TEXT_PLAIN)
          .body("Failed to retrieve entity: " + e.getMessage());
    }
  }

  @GetMapping("health")
  public ResponseEntity<String> checkHealth() {
    try {
      JSONObject healthResult = dataStore.checkHealth();

      if ("UP".equals(healthResult.getString("status"))) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(healthResult.toString());
      } else {
        return ResponseEntity.status(503)
            .contentType(MediaType.APPLICATION_JSON)
            .body(healthResult.toString());
      }
    } catch (Exception e) {
      JSONObject errorResponse = new JSONObject();
      errorResponse.put("status", "DOWN");
      JSONObject details = new JSONObject();
      details.put("error", e.getMessage());
      errorResponse.put("details", details);

      return ResponseEntity.status(503)
          .contentType(MediaType.APPLICATION_JSON)
          .body(errorResponse.toString());
    }
  }
}
