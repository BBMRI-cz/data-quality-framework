package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

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
  void countSecondaryEntities_capsEachPatientAtTwenty() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE specimen (specimen_id INTEGER, person_id INTEGER)");
    sqliteTemplate.execute(
        "WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 50)"
            + " INSERT INTO specimen SELECT i, 1 FROM n");
    sqliteTemplate.execute("INSERT INTO specimen VALUES (101, 2),(102, 2),(103, 2)");

    // 50 specimens of patient 1 count as 20, plus the 3 of patient 2.
    assertEquals(23, store.countSecondaryEntities());
  }

  @Test
  void countSecondaryEntities_addingPatientWithHundredSpecimens_raisesCountByTwenty() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE specimen (specimen_id INTEGER, person_id INTEGER)");
    sqliteTemplate.execute("INSERT INTO specimen VALUES (1, 1),(2, 1),(3, 1),(4, 1),(5, 1)");
    int baseline = store.countSecondaryEntities();
    sqliteTemplate.execute(
        "WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 100)"
            + " INSERT INTO specimen SELECT 1000 + i, 2 FROM n");

    // All hundred specimens belong to one patient and count as 20.
    assertEquals(20, store.countSecondaryEntities() - baseline);
  }

  @Test
  void countSecondaryEntities_excludesSpecimensWithoutPerson() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE specimen (specimen_id INTEGER, person_id INTEGER)");
    sqliteTemplate.execute("INSERT INTO specimen VALUES (1, 1),(2, 1),(3, NULL),(4, NULL)");

    assertEquals(2, store.countSecondaryEntities());
  }

  @Test
  void countSecondaryEntities_emptyTable_returnsZero() {
    OmopDataStore store = sqliteStore();
    sqliteTemplate.execute("CREATE TABLE specimen (specimen_id INTEGER, person_id INTEGER)");

    assertEquals(0, store.countSecondaryEntities());
  }

  @Test
  void countSecondaryEntities_queryFails_returnsNull() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
        .thenThrow(new RuntimeException("no such table: specimen"));
    OmopDataStore store = new OmopDataStore(jdbcTemplate);

    assertNull(store.countSecondaryEntities());
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
