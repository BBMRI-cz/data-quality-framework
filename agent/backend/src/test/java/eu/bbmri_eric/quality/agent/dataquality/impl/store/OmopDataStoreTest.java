package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.dto.DBStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OmopDataStoreTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  void executeQuery_emptyResult_returnsZeroCount() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(Collections.emptyList());
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    ResultDTO result = store.executeQuery("SELECT id FROM patients");

    assertEquals(0, result.rawResult());
    assertEquals("Person", result.entityType());
    assertTrue(result.idSet().isEmpty());
    assertNull(result.error());
  }

  @Test
  void executeQuery_scalarResult_returnsCount() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(Map.of("count", 42L)));
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    ResultDTO result = store.executeQuery("SELECT COUNT(*) AS count FROM patients");

    assertEquals(42, result.rawResult());
    assertTrue(result.idSet().isEmpty());
    assertNull(result.error());
  }

  @Test
  void executeQuery_rowsResult_returnsIds() {
    when(jdbcTemplate.queryForList(anyString()))
        .thenReturn(List.of(Map.of("id", "p1"), Map.of("id", "p2")));
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    ResultDTO result = store.executeQuery("SELECT id FROM patients");

    assertEquals(2, result.rawResult());
    assertTrue(result.idSet().containsAll(List.of("p1", "p2")));
    assertNull(result.error());
  }

  @Test
  void executeQuery_error_returnsErrorResult() {
    when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("syntax error"));
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    ResultDTO result = store.executeQuery("INVALID SQL");

    assertEquals("syntax error", result.error());
  }

  @Test
  void checkHealth_success_returnsUp() throws Exception {
    doNothing().when(jdbcTemplate).execute(anyString());
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    JSONObject health = store.checkHealth();

    assertEquals("UP", health.getString("status"));
  }

  @Test
  void checkHealth_failure_returnsDown() throws Exception {
    doThrow(new RuntimeException("connection refused")).when(jdbcTemplate).execute(anyString());
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    JSONObject health = store.checkHealth();

    assertEquals("DOWN", health.getString("status"));
    assertEquals("connection refused", health.getJSONObject("details").getString("error"));
  }

  @Test
  void checkHealthV2_success_returnsUp() {
    doNothing().when(jdbcTemplate).execute(anyString());
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    DatabaseHealthDTO health = store.checkHealthV2();

    assertEquals(DBStatus.UP, health.status);
    assertNull(health.error);
  }

  @Test
  void checkHealthV2_failure_returnsDown() {
    doThrow(new RuntimeException("connection refused")).when(jdbcTemplate).execute(anyString());
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    DatabaseHealthDTO health = store.checkHealthV2();

    assertEquals(DBStatus.DOWN, health.status);
    assertEquals("connection refused", health.error);
  }
}
