package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import org.json.JSONObject;

/** Provides connectivity to a database on which the Data Quality Checks should be executed. */
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

  /**
   * Executes a query against the connected database.
   *
   * @param query the query string to execute. e.g., SQL or CQL
   * @return the result of the query execution as a {@link ResultDTO}
   */
  ResultDTO executeQuery(String query);

  /**
   * Checks the health of the database connection.
   *
   * @return a {@link eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO} containing the
   *     current health status and metrics of the database connection
   */
  DatabaseHealthDTO checkHealthV2();
}
