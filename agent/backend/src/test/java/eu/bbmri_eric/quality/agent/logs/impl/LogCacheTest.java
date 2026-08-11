package eu.bbmri_eric.quality.agent.logs.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.quality.agent.logs.dto.LogEntryDTO;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogCacheTest {

  private static LogEntryDTO entry(String message) {
    return new LogEntryDTO(Instant.now(), "INFO", "test", message);
  }

  @Test
  void add_withinCapacity_keepsAllEntries() {
    LogCache cache = new LogCache(3);
    cache.add(entry("a"));
    cache.add(entry("b"));
    cache.add(entry("c"));

    List<LogEntryDTO> result = cache.getEntries();
    assertEquals(3, result.size());
  }

  @Test
  void add_beyondCapacity_evictsOldestEntries() {
    LogCache cache = new LogCache(2);
    cache.add(entry("first"));
    cache.add(entry("second"));
    cache.add(entry("third"));

    List<LogEntryDTO> result = cache.getEntries();
    assertEquals(2, result.size());
    assertEquals("second", result.get(0).getMessage());
    assertEquals("third", result.get(1).getMessage());
  }

  @Test
  void getEntries_returnsImmutableSnapshot() {
    LogCache cache = new LogCache(10);
    cache.add(entry("a"));

    List<LogEntryDTO> result = cache.getEntries();
    org.junit.jupiter.api.Assertions.assertThrows(
        UnsupportedOperationException.class, () -> result.add(entry("b")));
  }
}
