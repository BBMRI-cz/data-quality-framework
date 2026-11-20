package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import eu.bbmri_eric.quality.agent.dataquality.dto.DBStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.nio.file.Path;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@ExtendWith(MockitoExtension.class)
class OmopDataStoreTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @TempDir private Path tempDir;

  private JdbcTemplate sqliteTemplate;

  private OmopDataStore sqliteStore() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("test.db"));
    sqliteTemplate = new JdbcTemplate(dataSource);
    return new OmopDataStore(sqliteTemplate);
  }

  @Test
  void executeQuery_emptyResult_returnsZeroCount() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE person (person_id INTEGER)");

    ResultDTO result = store.executeQuery("SELECT person_id FROM person");

    assertEquals(0, result.rawResult());
    assertEquals("Person", result.entityType());
    assertTrue(result.idSet().isEmpty());
    assertNull(result.error());
  }

  @Test
  void executeQuery_scalarResult_returnsCount() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE person (person_id INTEGER)");
    sqliteTemplate.execute("INSERT INTO person VALUES (1),(2),(3)");

    ResultDTO result = store.executeQuery("SELECT COUNT(*) AS count FROM person");

    assertEquals(3, result.rawResult());
    assertTrue(result.idSet().isEmpty());
    assertNull(result.error());
  }

  @Test
  void executeQuery_scalarResult_countLabelIsCaseInsensitive() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE person (person_id INTEGER)");
    sqliteTemplate.execute("INSERT INTO person VALUES (1),(2)");

    ResultDTO result = store.executeQuery("SELECT COUNT(*) AS Count FROM person");

    assertEquals(2, result.rawResult());
    assertTrue(result.idSet().isEmpty());
    assertNull(result.error());
  }

  @Test
  void executeQuery_rowsResult_returnsIds() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE person (person_id TEXT)");
    sqliteTemplate.execute("INSERT INTO person VALUES ('p1'),('p2')");

    ResultDTO result = store.executeQuery("SELECT person_id FROM person");

    assertEquals(2, result.rawResult());
    assertTrue(result.idSet().containsAll(List.of("p1", "p2")));
    assertNull(result.error());
  }

  @Test
  void executeQuery_rowsResult_nullAndDuplicateIds_rowCountCountsRowsIdSetDedups() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE person (person_id INTEGER, name TEXT)");
    sqliteTemplate.execute("INSERT INTO person VALUES (1,'a'),(1,'b'),(NULL,'c'),(2,'d')");

    ResultDTO result = store.executeQuery("SELECT person_id, name FROM person");

    assertEquals(4, result.rawResult());
    assertEquals(2, result.idSet().size());
    assertTrue(result.idSet().containsAll(List.of("1", "2")));
    assertNull(result.error());
  }

  @Test
  void executeQuery_singleRowNumericNonCount_returnsId() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE person (person_id INTEGER)");
    sqliteTemplate.execute("INSERT INTO person VALUES (42)");

    ResultDTO result = store.executeQuery("SELECT person_id FROM person");

    assertEquals(1, result.rawResult());
    assertTrue(result.idSet().contains("42"));
    assertNull(result.error());
  }

  @Test
  void executeQuery_error_returnsErrorResult() {
    doThrow(new RuntimeException("syntax error"))
        .when(jdbcTemplate)
        .query(anyString(), any(RowCallbackHandler.class));
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
