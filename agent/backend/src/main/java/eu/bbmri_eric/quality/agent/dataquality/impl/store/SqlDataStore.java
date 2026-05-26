package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.DBStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
public class SqlDataStore implements DataStore {

  private final JdbcTemplate jdbcTemplate;

  SqlDataStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public JSONObject getEntity(String entityType, String id) {
    throw new UnsupportedOperationException("SQL data store does not support entity retrieval.");
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
      List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
      if (rows.isEmpty()) {
        return new ResultDTO(0, "", Collections.emptySet());
      }
      if (rows.size() == 1) {
        Map<String, Object> row = rows.get(0);
        if (row.size() == 1) {
          Object value = row.values().iterator().next();
          if (value instanceof Number number) {
            return new ResultDTO(number.intValue(), "", Collections.emptySet());
          }
        }
      }
      Set<String> idSet = new HashSet<>();
      for (Map<String, Object> row : rows) {
        Object firstValue = row.values().iterator().next();
        if (firstValue != null) {
          idSet.add(firstValue.toString());
        }
      }
      return new ResultDTO(rows.size(), "", idSet);
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
      return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM specimen", Integer.class);
    } catch (Exception e) {
      log.error("Failed to count secondary entities in SQL store: {}", e.getMessage(), e);
      return null;
    }
  }
}
