package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.DBStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;

@Slf4j
public class OmopDataStore implements DataStore {

  static final String PERSON_ID_FIRST_COLUMN_MESSAGE =
      "SQL checks must return person_id as the first column; the result counts distinct patients.";
  static final String NON_INTEGER_PERSON_ID_MESSAGE =
      "SQL checks must return whole-number person_id values; got: ";

  private static final String SELECT_PERSON_BY_ID = "SELECT * FROM person WHERE person_id = ?";
  private static final String COUNT_PERSONS = "SELECT COUNT(*) FROM person";
  private static final String COUNT_SPECIMENS = "SELECT COUNT(*) FROM specimen";
  private static final String HEALTH_CHECK_QUERY = "SELECT 1";

  private final JdbcTemplate jdbcTemplate;

  OmopDataStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public JSONObject getEntity(String entityType, String id) {
    try {
      Integer parsedId = Integer.valueOf(id);
      List<Map<String, Object>> rows = jdbcTemplate.queryForList(SELECT_PERSON_BY_ID, parsedId);
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
    try {
      checkDatabaseConnection();
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
    try {
      return jdbcTemplate.query(query, OmopDataStore::extractPatientIds);
    } catch (Exception e) {
      log.error("SQL query execution failed: {}", e.getMessage(), e);
      return new ResultDTO(e.getMessage());
    }
  }

  private static ResultDTO extractPatientIds(ResultSet rs) throws SQLException {
    requirePersonIdFirstColumn(rs);
    Set<String> ids = collectPatientIds(rs);
    return new ResultDTO(ids.size(), "Person", ids);
  }

  private static void requirePersonIdFirstColumn(ResultSet rs) throws SQLException {
    String firstColumn = rs.getMetaData().getColumnLabel(1);
    if (!"person_id".equalsIgnoreCase(firstColumn)) {
      throw new IllegalArgumentException(PERSON_ID_FIRST_COLUMN_MESSAGE);
    }
  }

  private static Set<String> collectPatientIds(ResultSet rs) throws SQLException {
    Set<String> ids = new HashSet<>();
    while (rs.next()) {
      Object value = JdbcUtils.getResultSetValue(rs, 1);
      if (value != null) {
        ids.add(canonicalPatientId(value));
      }
    }
    return ids;
  }

  private static String canonicalPatientId(Object value) {
    try {
      return new BigDecimal(value.toString().strip()).toBigIntegerExact().toString();
    } catch (NumberFormatException | ArithmeticException e) {
      throw new IllegalArgumentException(NON_INTEGER_PERSON_ID_MESSAGE + value, e);
    }
  }

  @Override
  public DatabaseHealthDTO checkHealthV2() {
    try {
      checkDatabaseConnection();
      return new DatabaseHealthDTO(DBStatus.UP, null, null);
    } catch (Exception e) {
      log.error("SQL health check V2 failed: {}", e.getMessage(), e);
      return new DatabaseHealthDTO(DBStatus.DOWN, e.getMessage(), null);
    }
  }

  private void checkDatabaseConnection() {
    jdbcTemplate.execute(HEALTH_CHECK_QUERY);
  }

  @Override
  public Integer countPatients() {
    try {
      return jdbcTemplate.queryForObject(COUNT_PERSONS, Integer.class);
    } catch (Exception e) {
      log.error("Failed to count patients in SQL store: {}", e.getMessage(), e);
      return null;
    }
  }

  @Override
  public Integer countSecondaryEntities() {
    try {
      return jdbcTemplate.queryForObject(COUNT_SPECIMENS, Integer.class);
    } catch (Exception e) {
      log.error("Failed to count secondary entities in SQL store: {}", e.getMessage(), e);
      return null;
    }
  }
}
