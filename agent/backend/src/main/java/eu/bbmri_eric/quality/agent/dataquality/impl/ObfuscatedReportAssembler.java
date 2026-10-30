package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckResultDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * Assembles the privacy-safe version of a report that is sent to the central server. All
 * check-result values are ratios of a noisy count over the noisy patient total, rounded to two
 * decimals; check identities are exposed only as a SHA-256 hash of the query text (never the query
 * or the check name); entity totals come from the noisy totals stored on the report.
 */
@Slf4j
@Component
class ObfuscatedReportAssembler {

  private final QualityCheckService qualityCheckService;

  ObfuscatedReportAssembler(QualityCheckService qualityCheckService) {
    this.qualityCheckService = qualityCheckService;
  }

  ObfuscatedReportDTO assemble(Report report) {
    Integer obfuscatedPatientCount = report.getObfuscatedNumberOfEntities();
    Integer obfuscatedSampleCount = report.getObfuscatedNumberOfSecondaryEntities();
    List<QualityCheckDTO> qualityCheckDTOS = qualityCheckService.findAll();
    List<QualityCheckResultDTO> results =
        toObfuscatedResults(report, obfuscatedPatientCount, qualityCheckDTOS);
    log.debug("Prepared obfuscated report {} with {} results", report.getId(), results.size());
    return new ObfuscatedReportDTO(results, obfuscatedPatientCount, obfuscatedSampleCount);
  }

  private static @NonNull List<QualityCheckResultDTO> toObfuscatedResults(
      Report report, Integer obfuscatedPatientCount, List<QualityCheckDTO> qualityCheckDTOS) {
    return (report.getResults() == null ? List.<Result>of() : report.getResults())
        .stream()
            .map(result -> toObfuscatedResult(result, obfuscatedPatientCount, qualityCheckDTOS))
            .collect(Collectors.toList());
  }

  private static QualityCheckResultDTO toObfuscatedResult(
      Result result, Integer obfuscatedPatientCount, List<QualityCheckDTO> qualityCheckDTOS) {
    Double ratio = ratio(result, obfuscatedPatientCount);
    Double boundedRatio = null;
    if (ratio != null) {
      double rounded = BigDecimal.valueOf(ratio).setScale(2, RoundingMode.HALF_UP).doubleValue();
      boundedRatio = Math.clamp(rounded, 0.0, 1.0);
    }
    return new QualityCheckResultDTO(
        checkIdWithStratum(result, qualityCheckDTOS), result.getCheckName(), boundedRatio);
  }

  private static Double ratio(Result result, Integer numberOfEntities) {
    if (Objects.isNull(numberOfEntities) || Objects.isNull(result.getObfuscatedValue())) {
      return null;
    }
    if (numberOfEntities == 0) {
      return 0.0;
    }
    return result.getObfuscatedValue() / numberOfEntities;
  }

  private static String checkIdWithStratum(Result result, List<QualityCheckDTO> qualityCheckDTOS) {
    String checkId = checkId(result, qualityCheckDTOS);
    if (result.getStratum() != null) {
      return "%s (%s)".formatted(checkId, result.getStratum());
    }
    return checkId;
  }

  private static String checkId(Result result, List<QualityCheckDTO> qualityCheckDTOS) {
    String query =
        qualityCheckDTOS.stream()
            .filter(qualityCheckDTO -> qualityCheckDTO.getId().equals(result.getCheckId()))
            .findFirst()
            .map(QualityCheckDTO::getQuery)
            .orElse(result.getCheckId().toString());
    return hashQuery(query);
  }

  private static String hashQuery(String query) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(query.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }
}
