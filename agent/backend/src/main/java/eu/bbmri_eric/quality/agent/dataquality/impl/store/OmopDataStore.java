package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.DBStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.JdbcUtils;

@Slf4j
public class OmopDataStore implements DataStore {

  static final String PERSON_ID_FIRST_COLUMN_MESSAGE =
      "SQL checks must return person_id as the first column; the result counts distinct patients.";

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
      String checkQuery = stripTrailingSemicolons(query);
      String firstColumn =
          jdbcTemplate.query(
              "SELECT * FROM (\n" + checkQuery + "\n) q WHERE 1 = 0",
              (ResultSetExtractor<String>) rs -> rs.getMetaData().getColumnLabel(1));
      if (!"person_id".equalsIgnoreCase(firstColumn)) {
        return new ResultDTO(PERSON_ID_FIRST_COLUMN_MESSAGE);
      }
      Set<String> idSet = new HashSet<>();
      jdbcTemplate.query(
          "SELECT DISTINCT q.person_id FROM (\n"
              + checkQuery
              + "\n) q JOIN person p ON p.person_id = q.person_id",
          (RowCallbackHandler) rs -> idSet.add(JdbcUtils.getResultSetValue(rs, 1).toString()));
      return new ResultDTO(idSet.size(), "Person", idSet);
    } catch (Exception e) {
      log.error("SQL query execution failed: {}", e.getMessage(), e);
      return new ResultDTO(e.getMessage());
    }
  }

  private static String stripTrailingSemicolons(String query) {
    String stripped = query.strip();
    while (stripped.endsWith(";")) {
      stripped = stripped.substring(0, stripped.length() - 1).strip();
    }
    return stripped;
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
      return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM specimen", Integer.class);
    } catch (Exception e) {
      log.error("Failed to count secondary entities in SQL store: {}", e.getMessage(), e);
      return null;
    }
  }
}
