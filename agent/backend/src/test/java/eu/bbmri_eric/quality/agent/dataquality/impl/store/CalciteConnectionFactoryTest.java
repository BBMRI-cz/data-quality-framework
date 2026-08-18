package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
