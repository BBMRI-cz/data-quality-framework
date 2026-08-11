package eu.bbmri_eric.quality.agent.logs;

import eu.bbmri_eric.quality.agent.logs.dto.LogEntryDTO;
import java.util.List;

public interface LogService {
  /**
   * Return the most recent log entries captured by the in-memory appender.
   *
   * @return an immutable list of the latest {@link LogEntryDTO} instances, never {@code null}
   */
  List<LogEntryDTO> getRecentLogs();
}
