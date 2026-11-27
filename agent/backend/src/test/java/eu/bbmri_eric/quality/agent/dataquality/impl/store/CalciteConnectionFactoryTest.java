package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class CalciteConnectionFactoryTest {

  @TempDir Path tempDir;

  @Test
  void directoryUrl_runsSqlQueriesOverCsvFiles() throws Exception {
    Files.writeString(
        tempDir.resolve("person.csv"),
        "\"person_id\",\"name\",\"age\"\n\"1\",\"Alice\",\"30\"\n\"2\",\"Bob\",\"25\"\n");
    CalciteConnectionFactory factory = new CalciteConnectionFactory();
    JdbcTemplate jdbcTemplate =
        factory.createJdbcTemplate("jdbc:calcite:directory=" + tempDir.toAbsolutePath());

    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM person", Integer.class);
    assertEquals(2, count);
  }

  @Test
  void directoryUrl_executesScalarCount() throws Exception {
    Files.writeString(tempDir.resolve("person.csv"), "\"person_id\"\n\"1\"\n\"2\"\n\"3\"\n");
    CalciteConnectionFactory factory = new CalciteConnectionFactory();
    JdbcTemplate jdbcTemplate =
        factory.createJdbcTemplate("jdbc:calcite:directory=" + tempDir.toAbsolutePath());

    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM person", Long.class);
    assertEquals(3, count);
  }

  @Test
  void directoryUrl_executeQueryCanonicalizesTextPersonIds() throws Exception {
    Files.writeString(
        tempDir.resolve("person.csv"),
        "\"person_id\"\n\"0999999999999999999999\"\n\"999999999999999999999\"\n");
    CalciteConnectionFactory factory = new CalciteConnectionFactory();
    JdbcTemplate jdbcTemplate =
        factory.createJdbcTemplate("jdbc:calcite:directory=" + tempDir.toAbsolutePath());

    ResultDTO result = new OmopDataStore(jdbcTemplate).executeQuery("SELECT person_id FROM person");

    assertEquals(1, result.rawResult());
    assertEquals(1, result.idSet().size());
    assertTrue(result.idSet().contains("999999999999999999999"));
    assertNull(result.error());
  }

  @Test
  void invalidUrl_throws() {
    CalciteConnectionFactory factory = new CalciteConnectionFactory();
    assertThrows(
        IllegalArgumentException.class, () -> factory.createJdbcTemplate("jdbc:postgresql://x"));
  }

  @Test
  void nullUrl_throws() {
    CalciteConnectionFactory factory = new CalciteConnectionFactory();
    assertThrows(IllegalArgumentException.class, () -> factory.createJdbcTemplate(null));
  }
}
