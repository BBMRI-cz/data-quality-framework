package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.DBStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.sql.ResultSetMetaData;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.JdbcUtils;

@Slf4j
public class OmopDataStore implements DataStore {

  /**
   * Per-person capped specimen count: the published total is the number of specimens attributable
   * to a patient, at most {@link DataStore#SECONDARY_ENTITY_SENSITIVITY} per patient. Specimens
   * without a {@code person_id} are not attributable to any patient and are excluded. Uses only
   * {@code COALESCE}/{@code CASE}/grouped subquery so it runs on SQLite, H2, PostgreSQL, MySQL and
   * Calcite alike.
   */
  private static final String COUNT_SECONDARY_ENTITIES_SQL =
      """
      SELECT COALESCE(SUM(CASE WHEN n > %d THEN %d ELSE n END), 0)
      FROM (SELECT COUNT(*) AS n FROM specimen WHERE person_id IS NOT NULL GROUP BY person_id) s
      """
          .formatted(
              DataStore.SECONDARY_ENTITY_SENSITIVITY, DataStore.SECONDARY_ENTITY_SENSITIVITY);

  private final JdbcTemplate jdbcTemplate;

  OmopDataStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public JSONObject getEntity(String entityType, String id) {
    if (jdbcTemplate == null) {
      throw new IllegalStateException("SQL data store not initialized");
    }
    try {
      Integer parsedId = Integer.valueOf(id);
      String idColumn = "person_id";
      List<Map<String, Object>> rows =
          jdbcTemplate.queryForList(
              "SELECT * FROM person" + " WHERE " + idColumn + " = ?", parsedId);
      if (rows.isEmpty()) {
        return null;
      }
      return new JSONObject(rows.getFirst());
    } catch (Exception e) {
      log.error("Failed to retrieve person with id {}: {}", id, e.getMessage(), e);
      throw new RuntimeException("Failed to retrieve entity: " + e.getMessage(), e);
    }
  }

  @Override
  public JSONObject checkHealth() {
    JSONObject response = new JSONObject();
    if (jdbcTemplate == null) {
      response.put("status", "DOWN");
      response.put("details", new JSONObject().put("error", "SQL data store not initialized"));
      return response;
    }
    try {
      jdbcTemplate.execute("SELECT 1");
      response.put("status", "UP");
      response.put("details", JSONObject.NULL);
    } catch (Exception e) {
      log.error("SQL health check failed: {}", e.getMessage(), e);
      response.put("status", "DOWN");
      response.put("details", new JSONObject().put("error", e.getMessage()));
    }
    return response;
  }

  @Override
  public ResultDTO executeQuery(String query) {
    if (jdbcTemplate == null) {
      return new ResultDTO("SQL data store not initialized");
    }
    try {
      int[] rowCount = new int[1];
      Number[] countValue = new Number[1];
      Set<String> idSet = new HashSet<>();
      jdbcTemplate.query(
          query,
          (RowCallbackHandler)
              rs -> {
                rowCount[0]++;
                Object firstValue = JdbcUtils.getResultSetValue(rs, 1);
                ResultSetMetaData metaData = rs.getMetaData();
                if (rowCount[0] == 1
                    && metaData.getColumnCount() == 1
                    && firstValue instanceof Number number
                    && metaData.getColumnLabel(1).toLowerCase().startsWith("count")) {
                  countValue[0] = number;
                } else {
                  countValue[0] = null;
                }
                if (firstValue != null) {
                  idSet.add(firstValue.toString());
                }
              });
      if (rowCount[0] == 0) {
        return new ResultDTO(0, "Person", Collections.emptySet());
      }
      if (countValue[0] != null) {
        return new ResultDTO(countValue[0].intValue(), "Person", Collections.emptySet());
      }
      return new ResultDTO(rowCount[0], "Person", idSet);
    } catch (Exception e) {
      log.error("SQL query execution failed: {}", e.getMessage(), e);
      return new ResultDTO(e.getMessage());
    }
  }

  @Override
  public DatabaseHealthDTO checkHealthV2() {
    if (jdbcTemplate == null) {
      return new DatabaseHealthDTO(DBStatus.DOWN, "SQL data store not initialized", null);
    }
    try {
      jdbcTemplate.execute("SELECT 1");
      return new DatabaseHealthDTO(DBStatus.UP, null, null);
    } catch (Exception e) {
      log.error("SQL health check V2 failed: {}", e.getMessage(), e);
      return new DatabaseHealthDTO(DBStatus.DOWN, e.getMessage(), null);
    }
  }

  @Override
  public Integer countPatients() {
    if (jdbcTemplate == null) {
      return null;
    }
    try {
      return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM person", Integer.class);
    } catch (Exception e) {
      log.error("Failed to count patients in SQL store: {}", e.getMessage(), e);
      return null;
    }
  }

  @Override
  public Integer countSecondaryEntities() {
    if (jdbcTemplate == null) {
      return null;
    }
    try {
      return jdbcTemplate.queryForObject(COUNT_SECONDARY_ENTITIES_SQL, Integer.class);
    } catch (Exception e) {
      log.error("Failed to count secondary entities in SQL store: {}", e.getMessage(), e);
      return null;
    }
  }
}
