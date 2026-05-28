package eu.bbmri_eric.quality.agent.dataquality.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class ReportMapperTest {

  private final ModelMapper modelMapper = new ModelMapper();

  @Test
  void mapReport_withAllFields_mapsIntoReportDTO() {
    int warningThreshold = 10;
    int errorThreshold = 30;

    Report report = new Report();
    report.setId(101L);
    report.setGeneratedAt(LocalDateTime.of(2025, 5, 12, 14, 30));
    report.setStatus(ReportStatus.GENERATED);
    report.setEpsilonBudget(1.5);
    report.setNumberOfEntities(250);
    report.setNumberOfSecondaryEntities(42);
    report.setResults(
        new ArrayList<>(
            List.of(
                createResult(
                    "Missing Gender attribute",
                    11L,
                    12,
                    12.5,
                    10,
                    30,
                    0.4,
                    null,
                    null,
                    Set.of("patient-1", "patient-2")),
                createResult(
                    "Missing Birth Date",
                    12L,
                    0,
                    0.0,
                    10,
                    30,
                    0.4,
                    "There is no error message",
                    "stratum_a",
                    Set.of("patient-3")))));

    ReportDTO reportDTO = modelMapper.map(report, ReportDTO.class);

    assertEquals(101L, reportDTO.getId());
    assertEquals(LocalDateTime.of(2025, 5, 12, 14, 30), reportDTO.getGeneratedAt());
    assertEquals(ReportStatus.GENERATED, reportDTO.getStatus());
    assertEquals(1.5, reportDTO.getEpsilonBudget());
    assertEquals(250, reportDTO.getNumberOfEntities());
    assertEquals(42, reportDTO.getNumberOfSecondaryEntities());
    assertEquals(2, reportDTO.getResults().size());

    var firstResult = reportDTO.getResults().getFirst();
    assertEquals("Missing Gender attribute", firstResult.getCheckName());
    assertEquals(11L, firstResult.getCheckId());
    assertEquals(12, firstResult.getRawValue());
    assertEquals(12.5, firstResult.getObfuscatedValue());
    assertEquals(10, firstResult.getWarningThreshold());
    assertEquals(30, firstResult.getErrorThreshold());
    assertEquals(0.4, firstResult.getEpsilon());
    assertNull(firstResult.getError());
    assertNull(firstResult.getStratum());
    assertTrue(firstResult.getPatients().containsAll(Set.of("patient-1", "patient-2")));

    var secondResult = reportDTO.getResults().get(1);
    assertEquals("Missing Birth Date", secondResult.getCheckName());
    assertEquals(12L, secondResult.getCheckId());
    assertEquals(0, secondResult.getRawValue());
    assertEquals(0.0, secondResult.getObfuscatedValue());
    assertEquals(10, secondResult.getWarningThreshold());
    assertEquals(30, secondResult.getErrorThreshold());
    assertEquals(0.4, secondResult.getEpsilon());
    assertEquals("There is no error message", secondResult.getError());
    assertEquals("stratum_a", secondResult.getStratum());
    assertEquals(Set.of("patient-3"), secondResult.getPatients());
  }

  @Test
  void mapReport_withNullResults_keepsResultsNull() {
    Report report = new Report();
    report.setId(102L);
    report.setResults(null);

    ReportDTO reportDTO = modelMapper.map(report, ReportDTO.class);

    assertEquals(102L, reportDTO.getId());
    assertNull(reportDTO.getResults());
  }

  @Test
  void mapReport_withEmptyResults_mapsToEmptyResultsList() {
    Report report = new Report();
    report.setId(103L);
    report.setResults(new ArrayList<>());

    ReportDTO reportDTO = modelMapper.map(report, ReportDTO.class);

    assertEquals(103L, reportDTO.getId());
    assertTrue(reportDTO.getResults().isEmpty());
  }

  @Test
  void mapReport_withNullOptionalFields_keepsNullValues() {
    Report report = new Report();
    report.setId(104L);
    report.setGeneratedAt(null);
    report.setStatus(null);
    report.setNumberOfEntities(null);
    report.setNumberOfSecondaryEntities(null);
    report.setResults(new ArrayList<>());

    ReportDTO reportDTO = modelMapper.map(report, ReportDTO.class);

    assertEquals(104L, reportDTO.getId());
    assertNull(reportDTO.getGeneratedAt());
    assertNull(reportDTO.getStatus());
    assertNull(reportDTO.getNumberOfEntities());
    assertNull(reportDTO.getNumberOfSecondaryEntities());
    assertTrue(reportDTO.getResults().isEmpty());
  }

  @Test
  void mapReport_doesNotModifyOriginalInput() {
    Report report = new Report();
    report.setId(105L);
    report.setGeneratedAt(LocalDateTime.of(2024, 1, 15, 9, 45));
    report.setStatus(ReportStatus.GENERATED);
    report.setEpsilonBudget(2.5);
    report.setNumberOfEntities(500);
    report.setNumberOfSecondaryEntities(17);
    report.setResults(
        new ArrayList<>(
            List.of(
                createResult(
                    "Check A", 21L, 7, 7.5, 10, 30, 0.5, null, null, Set.of("patient-x")))));

    ReportDTO reportDTO = modelMapper.map(report, ReportDTO.class);

    assertEquals(105L, report.getId());
    assertEquals(105L, reportDTO.getId());
    assertEquals(LocalDateTime.of(2024, 1, 15, 9, 45), report.getGeneratedAt());
    assertEquals(ReportStatus.GENERATED, report.getStatus());
    assertEquals(2.5, report.getEpsilonBudget());
    assertEquals(500, report.getNumberOfEntities());
    assertEquals(17, report.getNumberOfSecondaryEntities());
    assertEquals(1, report.getResults().size());
    assertEquals("Check A", reportDTO.getResults().getFirst().getCheckName());
  }

  private Result createResult(
      String checkName,
      Long checkId,
      Integer rawValue,
      Double obfuscatedValue,
      int warningThreshold,
      int errorThreshold,
      double epsilon,
      String error,
      String stratum,
      Set<String> patients) {
    Result result =
        new Result(
            checkName,
            checkId,
            rawValue,
            obfuscatedValue,
            warningThreshold,
            errorThreshold,
            epsilon,
            error,
            stratum);
    result.setPatients(patients);
    return result;
  }
}
