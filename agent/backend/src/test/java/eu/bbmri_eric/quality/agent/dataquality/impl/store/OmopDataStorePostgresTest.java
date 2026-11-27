package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class OmopDataStorePostgresTest {

  private static GenericContainer<?> postgres;
  private static OmopDataStore store;

  @BeforeAll
  static void setUp() {
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(), "Docker is not available");
    postgres =
        new GenericContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_DB", "cdm")
            .withEnv("POSTGRES_USER", "cdm")
            .withEnv("POSTGRES_PASSWORD", "cdm")
            .withExposedPorts(5432)
            .waitingFor(
                Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
    postgres.start();
    String url =
        "jdbc:postgresql://"
            + postgres.getHost()
            + ":"
            + postgres.getMappedPort(5432)
            + "/cdm?currentSchema=cdm";
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(url);
    dataSource.setUsername("cdm");
    dataSource.setPassword("cdm");
    JdbcTemplate template = new JdbcTemplate(dataSource);
    template.execute("CREATE SCHEMA cdm");
    template.execute("SET search_path TO cdm");
    template.execute("CREATE TABLE person (person_id BIGINT PRIMARY KEY)");
    template.execute(
        "CREATE TABLE condition_occurrence (person_id BIGINT, condition_concept_id BIGINT)");
    template.execute("INSERT INTO person VALUES (1),(2),(3)");
    template.execute(
        "INSERT INTO condition_occurrence VALUES (1,20),(1,21),(1,22),(2,20),(3,20),(99,20)");
    store = new OmopDataStore(template);
  }

  @AfterAll
  static void tearDown() {
    if (postgres != null) {
      postgres.stop();
    }
  }

  @Test
  void executeQuery_personQuery_returnsCount() {
    ResultDTO result = store.executeQuery("SELECT person_id FROM person");
    assertEquals(3, result.rawResult());
    assertTrue(result.idSet().containsAll(List.of("1", "2", "3")));
    assertNull(result.error());
  }

  @Test
  void executeQuery_trailingSemicolon_returnsCount() {
    ResultDTO result = store.executeQuery("SELECT person_id FROM person;");
    assertEquals(3, result.rawResult());
    assertNull(result.error());
  }

  @Test
  void executeQuery_multipleRowsPerPatient_countsDistinctPatients() {
    ResultDTO result = store.executeQuery("SELECT person_id FROM condition_occurrence");
    assertEquals(4, result.rawResult());
    assertEquals(4, result.idSet().size());
    assertTrue(result.idSet().containsAll(List.of("1", "2", "3", "99")));
    assertNull(result.error());
  }

  @Test
  void executeQuery_trailingSemicolonAndLineComment_returnsReadableDriverError() {
    ResultDTO result = store.executeQuery("SELECT person_id FROM person; -- all patients");
    assertNull(result.rawResult());
    assertTrue(result.error().endsWith("Multiple ResultSets were returned by the query."));
  }

  @Test
  void executeQuery_trailingSemicolonAndBlockComment_returnsReadableDriverError() {
    ResultDTO result = store.executeQuery("SELECT person_id FROM person; /* all patients */");
    assertNull(result.rawResult());
    assertTrue(result.error().endsWith("Multiple ResultSets were returned by the query."));
  }

  @Test
  void executeQuery_joinWithDuplicatePersonIdLabels_usesFirstColumn() {
    ResultDTO result =
        store.executeQuery(
            "SELECT p.person_id, co.person_id FROM person p JOIN condition_occurrence co"
                + " ON co.person_id = p.person_id");
    assertEquals(3, result.rawResult());
    assertEquals(3, result.idSet().size());
    assertNull(result.error());
  }

  @Test
  void executeQuery_quotedUpperCasePersonIdLabel_returnsCount() {
    ResultDTO result = store.executeQuery("SELECT person_id AS \"PERSON_ID\" FROM person");
    assertEquals(3, result.rawResult());
    assertNull(result.error());
  }

  @Test
  void executeQuery_countColumnFirst_failsWithPersonIdRequiredError() {
    ResultDTO result = store.executeQuery("SELECT COUNT(*) AS count FROM person");
    assertEquals(OmopDataStore.PERSON_ID_FIRST_COLUMN_MESSAGE, result.error());
    assertNull(result.rawResult());
  }
}
