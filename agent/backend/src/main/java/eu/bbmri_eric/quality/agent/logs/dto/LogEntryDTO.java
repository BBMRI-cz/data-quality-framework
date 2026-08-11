package eu.bbmri_eric.quality.agent.logs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Objects;

/** Immutable data transfer object representing a single captured log entry. */
@Schema(description = "A single log entry captured by the in-memory log appender")
public class LogEntryDTO {

  @Schema(
      description = "Timestamp when the log entry was created",
      example = "2026-08-11T12:00:00Z")
  private final Instant timestamp;

  @Schema(description = "Log level of the entry", example = "INFO")
  private final String level;

  @Schema(description = "Name of the logger that produced the entry")
  private final String loggerName;

  @Schema(description = "Formatted log message")
  private final String message;

  public LogEntryDTO(Instant timestamp, String level, String loggerName, String message) {
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    this.level = Objects.requireNonNull(level, "level must not be null");
    this.loggerName = loggerName;
    this.message = message;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getLevel() {
    return level;
  }

  public String getLoggerName() {
    return loggerName;
  }

  public String getMessage() {
    return message;
  }
}
