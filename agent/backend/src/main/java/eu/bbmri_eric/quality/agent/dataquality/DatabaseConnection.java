package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;

/**
 * Provides connectivity to a database on which the Data Quality Checks should be executed.
 */
public interface DatabaseConnection {

  /**
   * Executes a SQL query against the connected database.
   *
   * @param query the SQL query string to execute
   * @return the result of the query execution, typically a {@link java.sql.ResultSet} or an
   *     update count depending on the query type
   */
  Object executeQuery(String query);

  /**
   * Checks the health of the database connection.
   *
   * @return a {@link DatabaseHealthDTO} containing the current health status and metrics of the
   *     database connection
   */
  DatabaseHealthDTO checkHealth();
}
