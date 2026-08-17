package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Builds a {@link JdbcTemplate} backed by Apache Calcite so that standard SQL data quality checks
 * can run over CSV files.
 *
 * <p>Two connection URL forms are supported:
 *
 * <ul>
 *   <li>{@code jdbc:calcite:model=<model-file>} - a standard Calcite model (JSON/YAML) file that
 *       references the directory of CSV files.
 *   <li>{@code jdbc:calcite:directory=<dir>} - a filesystem directory containing CSV files. A CSV
 *       schema is generated on the fly for that directory.
 * </ul>
 */
@Slf4j
final class CalciteConnectionFactory {

  private static final String CALCITE_PREFIX = "jdbc:calcite:";
  private static final String DIRECTORY_PREFIX = CALCITE_PREFIX + "directory=";
  private static final String MODEL_PREFIX = CALCITE_PREFIX + "model=";

  JdbcTemplate createJdbcTemplate(String url) {
    String resolvedUrl = resolveUrl(url);
    log.info("Creating Calcite data store for URL: {}", resolvedUrl);
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setUrl(resolvedUrl);
    return new JdbcTemplate(dataSource);
  }

  private String resolveUrl(String url) {
    if (url == null) {
      throw new IllegalArgumentException("Calcite JDBC URL must not be null");
    }
    String trimmed = url.trim();
    if (trimmed.startsWith(DIRECTORY_PREFIX)) {
      String directory = trimmed.substring(DIRECTORY_PREFIX.length()).trim();
      if (directory.isEmpty()) {
        throw new IllegalArgumentException("Calcite JDBC URL is missing the CSV directory");
      }
      return buildInlineModelUrl(directory);
    }
    if (trimmed.startsWith(MODEL_PREFIX)) {
      return trimmed;
    }
    throw new IllegalArgumentException(
        "Unsupported Calcite JDBC URL: "
            + url
            + ". Expected jdbc:calcite:model=... or jdbc:calcite:directory=...");
  }

  private String buildInlineModelUrl(String directory) {
    String modelJson =
        "{"
            + "\"version\":\"1.0\","
            + "\"defaultSchema\":\"CSV\","
            + "\"schemas\":[{"
            + "\"name\":\"CSV\","
            + "\"type\":\"custom\","
            + "\"factory\":\"org.apache.calcite.adapter.csv.CsvSchemaFactory\","
            + "\"operand\":{\"directory\":\""
            + escape(directory)
            + "\"}"
            + "}]}";
    return CALCITE_PREFIX + "model=inline:" + modelJson + ";lex=MYSQL";
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
