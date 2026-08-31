package eu.bbmri_eric.quality.server.dataquality.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.ReportPdfService;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckResult;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Service implementation for generating PDF report summaries using Thymeleaf templating and
 * OpenHTMLToPDF.
 */
@Service
class ReportPdfServiceImpl implements ReportPdfService {

  private static final Logger logger = LoggerFactory.getLogger(ReportPdfServiceImpl.class);
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  private final SpringTemplateEngine templateEngine;
  private final ReportRepository reportRepository;
  private final AgentRepository agentRepository;
  private final ObjectProvider<BuildProperties> buildPropertiesProvider;

  ReportPdfServiceImpl(
      SpringTemplateEngine templateEngine,
      ReportRepository reportRepository,
      AgentRepository agentRepository,
      ObjectProvider<BuildProperties> buildPropertiesProvider) {
    this.templateEngine = templateEngine;
    this.reportRepository = reportRepository;
    this.agentRepository = agentRepository;
    this.buildPropertiesProvider = buildPropertiesProvider;
  }

  @Override
  public byte[] generateReportSummary(String reportId) {
    Report report =
        reportRepository
            .findById(reportId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException("Report with ID %s not found".formatted(reportId)));

    Agent agent = agentRepository.findById(report.getAgentId()).orElse(null);
    String agentName =
        (agent != null && agent.getName() != null) ? agent.getName() : "Unknown Agent";
    String agentVersion =
        (agent != null && agent.getVersion() != null) ? agent.getVersion() : "Unknown";

    List<ResultItem> results = mapResults(report.getQualityCheckResults());

    Context context = new Context();
    context.setVariable("reportId", report.getId());
    context.setVariable("reportTimestamp", formatTimestamp(report.getTimestamp()));
    context.setVariable("generatedAt", formatTimestamp(Instant.now()));
    context.setVariable("agentName", agentName);
    context.setVariable("agentId", report.getAgentId());
    context.setVariable("agentVersion", agentVersion);
    context.setVariable("totalPatients", report.getTotalPatients());
    context.setVariable("totalSamples", report.getTotalSamples());
    context.setVariable("totalChecks", results.size());
    context.setVariable("passedCount", countPassed(results));
    context.setVariable("warningCount", countWarning(results));
    context.setVariable("failedCount", countFailed(results));
    context.setVariable("platformVersion", getPlatformVersion());
    context.setVariable("logoBase64", loadLogoPng());
    context.setVariable("results", results);

    String html = templateEngine.process("report-summary", context);

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.withHtmlContent(html, null);
      builder.useFastMode();
      builder.toStream(outputStream);
      builder.run();
      return outputStream.toByteArray();
    } catch (Exception e) {
      logger.error("Failed to generate PDF for report {}", reportId, e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  private String formatTimestamp(Instant instant) {
    if (instant == null) {
      return "N/A";
    }
    return DATE_FORMATTER.format(instant);
  }

  private String getPlatformVersion() {
    BuildProperties props = buildPropertiesProvider.getIfAvailable();
    if (props != null && props.getVersion() != null) {
      return props.getVersion();
    }
    return "unknown";
  }

  private String loadLogoPng() {
    try (InputStream is = getClass().getResourceAsStream("/templates/logo.png")) {
      if (is == null) {
        logger.warn("Logo PNG not found in templates folder");
        return "";
      }
      return Base64.getEncoder().encodeToString(is.readAllBytes());
    } catch (IOException e) {
      logger.warn("Failed to load logo PNG", e);
      return "";
    }
  }

  private List<ResultItem> mapResults(List<QualityCheckResult> results) {
    List<ResultItem> items = new ArrayList<>();
    for (QualityCheckResult r : results) {
      QualityCheck check = r.getQualityCheck();
      Double value = r.getResult();
      String status;
      String resultDisplay;
      if (value == null) {
        status = "FAILED";
        resultDisplay = "N/A";
      } else {
        double percentage = value * 100.0;
        resultDisplay = formatPercent(percentage);
        if (percentage > check.getErrorThreshold()) {
          status = "FAILED";
        } else if (percentage > check.getWarningThreshold()) {
          status = "WARNING";
        } else {
          status = "PASSED";
        }
      }
      items.add(
          new ResultItem(
              check.getName(),
              r.getVersion().getHash(),
              resultDisplay,
              status,
              formatPercent(check.getWarningThreshold()),
              formatPercent(check.getErrorThreshold()),
              check.getDescription()));
    }
    return items.stream()
        .sorted(
            (a, b) -> {
              int orderA = statusOrder(a.status);
              int orderB = statusOrder(b.status);
              int cmp = Integer.compare(orderA, orderB);
              return cmp != 0 ? cmp : a.name.compareToIgnoreCase(b.name);
            })
        .toList();
  }

  private String formatPercent(double value) {
    if (value == Math.floor(value)) {
      return "%.0f%%".formatted(value);
    }
    return "%.1f%%".formatted(value);
  }

  private int statusOrder(String status) {
    return switch (status) {
      case "FAILED" -> 0;
      case "WARNING" -> 1;
      default -> 2;
    };
  }

  private long countPassed(List<ResultItem> results) {
    return results.stream().filter(r -> "PASSED".equals(r.status)).count();
  }

  private long countWarning(List<ResultItem> results) {
    return results.stream().filter(r -> "WARNING".equals(r.status)).count();
  }

  private long countFailed(List<ResultItem> results) {
    return results.stream().filter(r -> "FAILED".equals(r.status)).count();
  }

  record ResultItem(
      String name,
      String hash,
      String resultDisplay,
      String status,
      String warningThresholdDisplay,
      String errorThresholdDisplay,
      String description) {}
}
