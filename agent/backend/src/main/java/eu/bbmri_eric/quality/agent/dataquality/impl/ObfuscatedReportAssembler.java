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
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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
    List<QualityCheckDTO> qualityChecks = qualityCheckService.findAll();
    Map<Long, String> queriesByCheckId = new HashMap<>();
    for (QualityCheckDTO qualityCheck : qualityChecks) {
      queriesByCheckId.put(qualityCheck.getId(), qualityCheck.getQuery());
    }
    MessageDigest sha256 = newSha256Digest();
    List<QualityCheckResultDTO> results =
        (report.getResults() == null ? List.<Result>of() : report.getResults())
            .stream()
                .map(
                    result ->
                        toObfuscatedResult(
                            result,
                            report.getObfuscatedNumberOfEntities(),
                            queriesByCheckId,
                            sha256))
                .toList();
    log.debug("Prepared obfuscated report {} with {} results", report.getId(), results.size());
    return new ObfuscatedReportDTO(
        results,
        report.getObfuscatedNumberOfEntities(),
        report.getObfuscatedNumberOfSecondaryEntities());
  }

  private static QualityCheckResultDTO toObfuscatedResult(
      Result result,
      Integer obfuscatedPatientCount,
      Map<Long, String> queriesByCheckId,
      MessageDigest sha256) {
    Double ratio = ratio(result, obfuscatedPatientCount);
    Double boundedRatio = null;
    if (ratio != null) {
      double rounded = BigDecimal.valueOf(ratio).setScale(2, RoundingMode.HALF_UP).doubleValue();
      boundedRatio = Math.clamp(rounded, 0.0, 1.0);
    }
    return new QualityCheckResultDTO(
        checkIdWithStratum(result, queriesByCheckId, sha256), result.getCheckName(), boundedRatio);
  }

  private static Double ratio(Result result, Integer numberOfEntities) {
    if (numberOfEntities == null || result.getObfuscatedValue() == null) {
      return null;
    }
    if (numberOfEntities == 0) {
      return 0.0;
    }
    return result.getObfuscatedValue() / numberOfEntities;
  }

  private static String checkIdWithStratum(
      Result result, Map<Long, String> queriesByCheckId, MessageDigest sha256) {
    String query =
        queriesByCheckId.getOrDefault(result.getCheckId(), result.getCheckId().toString());
    String checkId = hexHash(sha256, query);
    return result.getStratum() != null
        ? "%s (%s)".formatted(checkId, result.getStratum())
        : checkId;
  }

  private static MessageDigest newSha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is guaranteed by the JCA on every JVM; a library bug would stop startup anyway.
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private static String hexHash(MessageDigest digest, String text) {
    return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
  }
}
