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

  private final int maxSize;
  private final Deque<LogEntryDTO> entries = new ArrayDeque<>();

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
