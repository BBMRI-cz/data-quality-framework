package eu.bbmri_eric.quality.agent.logs.impl;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import eu.bbmri_eric.quality.agent.logs.dto.LogEntryDTO;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logback appender that forwards every received logging event into the shared {@link LogCache},
 * keeping a bounded window of the most recent log entries available for inspection via the API.
 */
final class LogbackCapturingAppender extends AppenderBase<ILoggingEvent> {

  private final AtomicLong idGenerator = new AtomicLong();

  private final LogCache logCache;

  LogbackCapturingAppender(LogCache logCache) {
    this.logCache = logCache;
  }

  @Override
  protected void append(ILoggingEvent event) {
    LogEntryDTO entry =
        new LogEntryDTO(
            idGenerator.incrementAndGet(),
            java.time.Instant.ofEpochMilli(event.getTimeStamp()),
            event.getLevel().toString(),
            event.getLoggerName(),
            event.getFormattedMessage());
    logCache.add(entry);
  }
}
