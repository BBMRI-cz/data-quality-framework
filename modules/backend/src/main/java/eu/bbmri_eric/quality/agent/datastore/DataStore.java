package eu.bbmri_eric.quality.agent.datastore;

import org.json.JSONObject;

public interface DataStore {
  /**
   * Retrieves an entity of the specified type and ID from the data store.
   *
   * @param entityType The type of the entity to retrieve (e.g., "Patient").
   * @param id The unique identifier of the entity.
   * @return A JSONObject representing the entity.
   * @throws Exception If an error occurs while retrieving the entity.
   */
  JSONObject getEntity(String entityType, String id) throws Exception;

  JSONObject checkHealth() throws Exception;
}
