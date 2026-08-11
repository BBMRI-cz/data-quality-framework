package eu.bbmri_eric.quality.agent.logs.impl;

import eu.bbmri_eric.quality.agent.logs.LogService;
import eu.bbmri_eric.quality.agent.logs.dto.LogEntryDTO;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service implementation exposing the recent log entries stored in the in-memory log cache. */
@Service
class LogServiceImpl implements LogService {

  private final LogCache logCache;

  LogServiceImpl(LogCache logCache) {
    this.logCache = logCache;
  }

  @Override
  public List<LogEntryDTO> getRecentLogs() {
    return logCache.getEntries();
  }
}
