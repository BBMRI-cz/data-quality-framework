package eu.bbmri_eric.quality.agent.dataquality.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ReportTest {
  @Test
  void testSetAndGetId() {
    Report report = new Report();
    report.setId(123L);
    assertEquals(123L, report.getId());
  }

  @Test
  void testSetAndGetStatus() {
    Report report = new Report();
    report.setStatus(ReportStatus.GENERATED);
    assertEquals(ReportStatus.GENERATED, report.getStatus());
  }
}
