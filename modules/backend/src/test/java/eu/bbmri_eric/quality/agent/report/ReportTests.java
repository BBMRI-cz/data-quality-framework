package eu.bbmri_eric.quality.agent.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ReportTests {
  @Test
  void testSetAndGetId() {
    Report report = new Report();
    report.setId(123L);
    assertEquals(123L, report.getId());
  }

  @Test
  void testSetAndGetStatus() {
    Report report = new Report();
    report.setStatus(Status.GENERATED);
    assertEquals(Status.GENERATED, report.getStatus());
  }
}
