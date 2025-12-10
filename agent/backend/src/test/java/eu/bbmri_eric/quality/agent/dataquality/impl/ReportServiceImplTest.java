package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.CQLQueryService;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.CQLQueryDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ReportServiceImplTest {

  @Autowired private ReportRepository reportRepository;
  @Autowired private ReportService reportService;
  @MockitoBean private CQLQueryService cqlQueryService;

  private Report testReport;

  @BeforeEach
  void setUp() {
    reportRepository.deleteAll();

    CQLQueryDTO mockQuery1 =
        new CQLQueryDTO(
            1L, "Query 1", "Description 1", "library Query1 version '1.0.0'", 10, 30, 1.0f);

    CQLQueryDTO mockQuery2 =
        new CQLQueryDTO(
            2L, "Query 2", "Description 2", "library Query2 version '1.0.0'", 10, 30, 1.0f);

    when(cqlQueryService.findAll()).thenReturn(List.of(mockQuery1, mockQuery2));
    when(cqlQueryService.findById(1L)).thenReturn(mockQuery1);
    when(cqlQueryService.findById(2L)).thenReturn(mockQuery2);

    testReport = new Report();
    testReport.setNumberOfEntities(1000);
    testReport.setStatus(ReportStatus.GENERATED);

    addResultToReport(testReport, "Check 1", 1L, 150, 155.5, null);
    addResultToReport(testReport, "Check 2", 1L, 75, 73.2, null);
    addResultToReport(testReport, "Check 3", 1L, 5, 0.0, null);
    addResultToReport(testReport, "Check 4", 2L, 200, 205.8, "stratum_A");
    addResultToReport(testReport, "Check 5", 2L, 8, 0.0, "stratum_B");

    testReport = reportRepository.save(testReport);
  }

  private void addResultToReport(
      Report report,
      String checkName,
      Long checkId,
      int rawValue,
      double obfuscatedValue,
      String stratum) {
    Result result =
        new Result(checkName, checkId, rawValue, obfuscatedValue, 10, 30, 1.0f, null, stratum);
    report.addResult(result);
  }

  @Test
  void generateReport() {
    reportRepository.deleteAll();
    assertThat(reportRepository.findAll()).isEmpty();
    reportService.generateReport();
    assertThat(reportRepository.findAll()).hasSize(1);
    Report report = reportRepository.findAll().getFirst();
    assertThat(report.getId()).isNotNull();
  }

  @Nested
  @DisplayName("Differential Privacy Obfuscation")
  class DifferentialPrivacyObfuscation {

    @Test
    @DisplayName("should never expose raw count values")
    void shouldNeverExposeRawValues() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      double[] returnedValues =
          reportDTO.getResults().stream().mapToDouble(QualityCheckResultDTO::getResult).toArray();

      double[] expectedRawProportions = {0.15, 0.075, 0.005, 0.20, 0.008};

      for (int i = 0; i < returnedValues.length; i++) {
        boolean isDifferent = Math.abs(returnedValues[i] - expectedRawProportions[i]) > 0.0001;
        boolean isSuppressed = returnedValues[i] == 0.0;

        assertThat(isDifferent || isSuppressed)
            .as("Value at index %d should be obfuscated or suppressed", i)
            .isTrue();
      }
    }

    @Test
    @DisplayName("should apply noise to all non-suppressed values")
    void shouldApplyNoiseToValues() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      QualityCheckResultDTO check1 = reportDTO.getResults().get(0);
      assertThat(check1.getResult()).isEqualTo(0.16);

      QualityCheckResultDTO check2 = reportDTO.getResults().get(1);
      assertThat(check2.getResult()).isEqualTo(0.07);
    }

    @Test
    @DisplayName("should suppress low counts (return 0.0)")
    void shouldSuppressLowCounts() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      QualityCheckResultDTO check3 = reportDTO.getResults().get(2);
      assertThat(check3.getResult()).isEqualTo(0.0);

      QualityCheckResultDTO check5 = reportDTO.getResults().get(4);
      assertThat(check5.getResult()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("should use obfuscated values, not raw values")
    void shouldUseObfuscatedValues() {
      Report report = new Report();
      report.setNumberOfEntities(100);
      report.setStatus(ReportStatus.GENERATED);

      addResultToReport(report, "Test Check", 1L, 50, 60.0, null);
      Report savedReport = reportRepository.save(report);

      ReportDTO reportDTO = reportService.getById(savedReport.getId());

      assertThat(reportDTO.getResults().get(0).getResult()).isEqualTo(0.6);
    }

    @Test
    @DisplayName("should handle reports with zero entities (0/0 case)")
    void shouldHandleZeroEntities() {
      Report report = new Report();
      report.setNumberOfEntities(0);
      report.setStatus(ReportStatus.GENERATED);
      addResultToReport(report, "Test Check", 1L, 0, 0.0, null);
      Report savedReport = reportRepository.save(report);

      ReportDTO reportDTO = reportService.getById(savedReport.getId());

      assertThat(reportDTO.getResults()).hasSize(1);
      assertThat(reportDTO.getResults().get(0).getResult()).isEqualTo(0.0);
    }

    @Test
    @DisplayName(
        "should protect against inference attacks - multiple queries don't reveal exact values")
    void shouldProtectAgainstInferenceAttacks() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      QualityCheckResultDTO check1 = reportDTO.getResults().get(0);
      double noisyProportion = check1.getResult();
      double noisyCount = noisyProportion * 1000;

      assertThat(noisyCount).isNotEqualTo(150.0);
      assertThat(noisyCount).isGreaterThan(0.0);
    }
  }

  @Nested
  @DisplayName("Check ID Formatting")
  class CheckIdFormatting {

    @Test
    @DisplayName("should create hash for check ID")
    void shouldCreateHashForCheckId() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      for (QualityCheckResultDTO result : reportDTO.getResults()) {
        assertThat(result.getHash())
            .as("Check ID hash should not be null or empty")
            .isNotNull()
            .isNotEmpty();
      }
    }

    @Test
    @DisplayName("should hash CQL query content, not just ID")
    void shouldHashQueryContent() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      String hash = reportDTO.getResults().get(0).getHash();
      assertThat(hash).matches("[a-f0-9]{64}");
    }

    @Test
    @DisplayName("should append stratum to hash when present")
    void shouldAppendStratumToHash() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      QualityCheckResultDTO check4 = reportDTO.getResults().get(3);
      assertThat(check4.getHash()).contains("stratum_A");
      assertThat(check4.getHash()).matches(".+ \\(stratum_A\\)");

      QualityCheckResultDTO check5 = reportDTO.getResults().get(4);
      assertThat(check5.getHash()).contains("stratum_B");
      assertThat(check5.getHash()).matches(".+ \\(stratum_B\\)");
    }

    @Test
    @DisplayName("should not append stratum when null")
    void shouldNotAppendStratumWhenNull() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      QualityCheckResultDTO check1 = reportDTO.getResults().get(0);
      assertThat(check1.getHash()).doesNotContain("(");
      assertThat(check1.getHash()).doesNotContain(")");
    }

    @Test
    @DisplayName("should use same hash for same check ID")
    void shouldUseSameHashForSameCheckId() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      String hash1 = reportDTO.getResults().get(0).getHash();
      String hash2 = reportDTO.getResults().get(1).getHash();
      String hash3 = reportDTO.getResults().get(2).getHash();

      assertThat(hash1).isEqualTo(hash2);
      assertThat(hash2).isEqualTo(hash3);
    }

    @Test
    @DisplayName("should use different hash for different check IDs")
    void shouldUseDifferentHashForDifferentCheckIds() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      String hash1Base = reportDTO.getResults().get(0).getHash();
      String hash4WithStratum = reportDTO.getResults().get(3).getHash();

      String hash4Base = hash4WithStratum.replaceAll(" \\(.*\\)", "");

      assertThat(hash1Base).isNotEqualTo(hash4Base);
    }
  }

  @Nested
  @DisplayName("Get by ID")
  class GetById {

    @Test
    @DisplayName("should return report with obfuscated results")
    void shouldReturnReportWithObfuscatedResults() {
      ReportDTO reportDTO = reportService.getById(testReport.getId());

      assertThat(reportDTO.getResults()).hasSize(5);
      assertThat(reportDTO.getResults().get(0).getResult()).isGreaterThan(0.0);
    }
  }
}
