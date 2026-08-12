package eu.bbmri_eric.quality.agent.logs.impl;

import eu.bbmri_eric.quality.agent.logs.dto.LogEntryDTO;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe, bounded in-memory buffer that retains only the most recent {@link LogEntryDTO}
 * instances (oldest kept entries are evicted once the configured capacity is exceeded).
 */
final class LogCache {

  private static final int DEFAULT_MAX_SIZE = 500;
  private static final LogCache SHARED = new LogCache(DEFAULT_MAX_SIZE);

  private final int maxSize;
  private final Deque<LogEntryDTO> entries = new ArrayDeque<>();

  /** Returns the single cache shared by every Spring context and the log appender. */
  static LogCache getShared() {
    return SHARED;
  }

  LogCache(int maxSize) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize must be positive, but was " + maxSize);
    }
    this.maxSize = maxSize;
  }

  synchronized void add(LogEntryDTO entry) {
    while (entries.size() >= maxSize) {
      entries.pollFirst();
    }
    entries.addLast(entry);
  }

  synchronized List<LogEntryDTO> getEntries() {
    return List.copyOf(entries);
  }
}
