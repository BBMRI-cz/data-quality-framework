package eu.bbmri_eric.quality.agent.config;

import jakarta.annotation.PreDestroy;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Component that automatically deletes the development database file on application shutdown.
 *
 * <p>This ensures a clean state for the next development session by removing the database file when
 * the application terminates, preventing issues with schema changes and stale data.
 *
 * <p>This component only runs when the "dev" Spring profile is active.
 */
@Component
@Profile("dev")
class DevDatabaseCleaner {

  private static final Logger logger = LoggerFactory.getLogger(DevDatabaseCleaner.class);

  @Value("${spring.datasource.url}")
  private String datasourceUrl;

  /**
   * Deletes the development database file on shutdown if it exists.
   *
   * <p>Extracts the database file path from the JDBC URL and attempts to delete it. This runs
   * during application shutdown before the JVM terminates.
   */
  @PreDestroy
  void cleanDevDatabase() {
    try {
      String dbPath = datasourceUrl.replace("jdbc:sqlite:", "");
      // Remove URL parameters if present (e.g., ?busy_timeout=10000)
      int paramIndex = dbPath.indexOf('?');
      if (paramIndex != -1) {
        dbPath = dbPath.substring(0, paramIndex);
      }

      // Delete main database file
      File dbFile = new File(dbPath);
      if (dbFile.exists()) {
        boolean deleted = dbFile.delete();
        if (deleted) {
          logger.info("Development database '{}' deleted successfully on shutdown", dbPath);
        }
      }

      // Delete SQLite Write-Ahead Log file (-wal)
      File walFile = new File(dbPath + "-wal");
      if (walFile.exists()) {
        boolean deleted = walFile.delete();
        if (deleted) {
          logger.info("Development database WAL file '{}' deleted successfully", walFile.getName());
        }
      }

      // Delete SQLite Shared Memory file (-shm)
      File shmFile = new File(dbPath + "-shm");
      if (shmFile.exists()) {
        boolean deleted = shmFile.delete();
        if (deleted) {
          logger.info("Development database SHM file '{}' deleted successfully", shmFile.getName());
        }
      }
    } catch (Exception e) {
      logger.error("Error while attempting to clean development database", e);
    }
  }
}
