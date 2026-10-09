package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import java.io.StringWriter;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditLogCsvWriterTest {

  private static final String BOM = "\uFEFF";
  private static final String HEADER =
      "id,timestamp,actor,actorId,action,module,entityId,details\r\n";

  private StringWriter output;
  private AuditLogCsvWriter csvWriter;

  @BeforeEach
  void setUp() {
    output = new StringWriter();
    csvWriter = new AuditLogCsvWriter(output);
  }

  @Test
  void writeHeader_writesByteOrderMarkAndColumnNames() {
    csvWriter.writeHeader();

    assertThat(output.toString()).isEqualTo(BOM + HEADER);
  }

  @Test
  void write_fullEntry_writesAllColumnsInHeaderOrder() {
    csvWriter.write(
        entry(1L, "admin", 7L, AuditAction.QUALITY_CHECK_UPDATED, "dataquality", 42L, "Updated"));

    assertThat(output.toString())
        .isEqualTo(
            "1,2026-09-15T10:30:05,admin,7,QUALITY_CHECK_UPDATED,dataquality,42,Updated\r\n");
  }

  @Test
  void write_nullOptionalFields_writesEmptyCells() {
    csvWriter.write(entry(2L, "SYSTEM", null, AuditAction.REPORT_GENERATED, null, null, null));

    assertThat(output.toString())
        .isEqualTo("2,2026-09-15T10:30:05,SYSTEM,,REPORT_GENERATED,,,\r\n");
  }

  @Test
  void write_valueWithCommaQuoteOrNewline_quotesAndEscapesIt() {
    csvWriter.write(
        entry(3L, "admin", 7L, AuditAction.OTHER, "user", null, "Said \"hi\", then\nleft"));

    assertThat(output.toString())
        .isEqualTo("3,2026-09-15T10:30:05,admin,7,OTHER,user,,\"Said \"\"hi\"\", then\nleft\"\r\n");
  }

  @Test
  void write_valueStartingWithFormulaCharacter_prefixesApostrophe() {
    csvWriter.write(
        entry(4L, "=HYPERLINK(\"x\")", null, AuditAction.LOGIN_FAILURE, "user", null, "@sum"));

    assertThat(output.toString())
        .isEqualTo(
            "4,2026-09-15T10:30:05,\"'=HYPERLINK(\"\"x\"\")\",,LOGIN_FAILURE,user,,'@sum\r\n");
  }

  @Test
  void write_valuesStartingWithPlusMinusOrTab_arePrefixed() {
    csvWriter.write(entry(5L, "+admin", null, AuditAction.OTHER, "-mod", null, "\tdetails"));

    assertThat(output.toString())
        .isEqualTo("5,2026-09-15T10:30:05,'+admin,,OTHER,'-mod,,'\tdetails\r\n");
  }

  private AuditLogDTO entry(
      Long id,
      String actor,
      Long actorId,
      AuditAction action,
      String module,
      Long entityId,
      String details) {
    return AuditLogDTO.builder()
        .id(id)
        .timestamp(LocalDateTime.of(2026, 9, 15, 10, 30, 5))
        .actor(actor)
        .actorId(actorId)
        .action(action)
        .module(module)
        .entityId(entityId)
        .details(details)
        .build();
  }
}
