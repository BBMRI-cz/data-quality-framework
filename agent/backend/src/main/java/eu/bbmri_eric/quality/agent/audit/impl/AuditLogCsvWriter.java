package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Writes audit log entries as RFC 4180 CSV.
 *
 * <p>The output starts with a UTF-8 byte order mark so spreadsheet applications detect the
 * encoding. Cells starting with a formula character are prefixed with an apostrophe, because some
 * values (e.g. the username of a failed login) are user-supplied and would otherwise be evaluated
 * as formulas when the file is opened in a spreadsheet.
 */
final class AuditLogCsvWriter {

  private static final String BYTE_ORDER_MARK = "\uFEFF";
  private static final String LINE_SEPARATOR = "\r\n";
  private static final String FORMULA_CHARACTERS = "=+-@\t\r";
  private static final String QUOTED_CHARACTERS = ",\"\r\n";
  private static final Object[] HEADER = {
    "id", "timestamp", "actor", "actorId", "action", "module", "entityId", "details"
  };
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final Writer writer;

  AuditLogCsvWriter(Writer writer) {
    this.writer = Objects.requireNonNull(writer, "writer");
  }

  void writeHeader() {
    append(BYTE_ORDER_MARK);
    writeLine(HEADER);
  }

  void write(AuditLogDTO entry) {
    writeLine(
        entry.getId(),
        entry.getTimestamp() == null ? null : TIMESTAMP_FORMAT.format(entry.getTimestamp()),
        entry.getActor(),
        entry.getActorId(),
        entry.getAction(),
        entry.getModule(),
        entry.getEntityId(),
        entry.getDetails());
  }

  private void writeLine(Object... values) {
    append(
        Arrays.stream(values).map(AuditLogCsvWriter::toCell).collect(Collectors.joining(","))
            + LINE_SEPARATOR);
  }

  private static String toCell(Object value) {
    String text = Objects.toString(value, "");
    if (!text.isEmpty() && FORMULA_CHARACTERS.indexOf(text.charAt(0)) >= 0) {
      text = "'" + text;
    }
    if (text.chars().anyMatch(c -> QUOTED_CHARACTERS.indexOf(c) >= 0)) {
      return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    return text;
  }

  private void append(String text) {
    try {
      writer.write(text);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write audit log CSV", ex);
    }
  }
}
