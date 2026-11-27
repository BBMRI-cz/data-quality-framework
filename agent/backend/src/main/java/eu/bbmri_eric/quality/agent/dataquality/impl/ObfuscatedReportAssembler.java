package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.PrivacyMetadataDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PrivacyBudget;
import eu.bbmri_eric.quality.agent.dataquality.privacy.PrivacyParameters;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
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
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/**
 * Assembles the privacy-safe version of a report that is sent to the central server. All
 * check-result values are ratios of a noisy count over the noisy patient total, rounded to two
 * decimals; check identities are exposed only as a SHA-256 hash of the query text (never the query
 * or the check name); entity totals come from the noisy totals stored on the report.
 *
 * <p>The payload also carries data-independent interpretation metadata: the agent version, the
 * configured privacy parameters with their budget split, and the epsilon and sensitivity of every
 * released value.
 */
@Slf4j
@Component
class ObfuscatedReportAssembler {

  private final QualityCheckService qualityCheckService;
  private final SettingsService settingsService;
  private final BuildProperties buildProperties;

  ObfuscatedReportAssembler(
      QualityCheckService qualityCheckService,
      SettingsService settingsService,
      BuildProperties buildProperties) {
    this.qualityCheckService = qualityCheckService;
    this.settingsService = settingsService;
    this.buildProperties = buildProperties;
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
    ObfuscatedReportDTO dto =
        new ObfuscatedReportDTO(
            results,
            report.getObfuscatedNumberOfEntities(),
            report.getObfuscatedNumberOfSecondaryEntities());
    addInterpretationMetadata(dto, report);
    return dto;
  }

  /**
   * Adds the data-independent metadata a recipient needs to interpret the released values: agent
   * version, configured privacy parameters with the budget split, and per-value
   * epsilon/sensitivity. Totals metadata is only present when the total itself was released.
   */
  private void addInterpretationMetadata(ObfuscatedReportDTO dto, Report report) {
    SettingsDTO settings = settingsService.getSettings();
    PrivacyParameters parameters = PrivacyParameters.from(settings);
    dto.setAgentVersion(buildProperties.getVersion());
    dto.setPrivacy(privacyMetadata(settings, parameters));
    double epsilonPerTotal = PrivacyBudget.epsilonPerEntityTotal(parameters.epsilon());
    if (report.getObfuscatedNumberOfEntities() != null) {
      dto.setTotalPatientsEpsilon(epsilonPerTotal);
      dto.setTotalPatientsSensitivity(ObfuscationStep.PATIENT_SENSITIVITY);
    }
    if (report.getObfuscatedNumberOfSecondaryEntities() != null) {
      dto.setTotalSamplesEpsilon(epsilonPerTotal);
      dto.setTotalSamplesSensitivity(ObfuscationStep.SECONDARY_ENTITY_SENSITIVITY);
    }
  }

  private static PrivacyMetadataDTO privacyMetadata(
      SettingsDTO settings, PrivacyParameters parameters) {
    PrivacyMetadataDTO privacy = new PrivacyMetadataDTO();
    privacy.setMechanism(parameters.mechanism().name());
    privacy.setEpsilon(parameters.epsilon());
    privacy.setDelta(parameters.delta());
    privacy.setMinThreshold(settings.getMinThreshold());
    privacy.setCheckResultsEpsilon(PrivacyBudget.epsilonForCheckResults(parameters.epsilon()));
    privacy.setEntityTotalEpsilon(PrivacyBudget.epsilonPerEntityTotal(parameters.epsilon()));
    privacy.setEntityTotalMechanism(ObfuscationStep.ENTITY_TOTAL_MECHANISM.name());
    return privacy;
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
        checkIdWithStratum(result, queriesByCheckId, sha256),
        result.getCheckName(),
        boundedRatio,
        result.getEpsilon(),
        ObfuscationStep.RESULT_SENSITIVITY);
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
    String query = queriesByCheckId.get(result.getCheckId());
    if (query == null) {
      query = result.getCheckId().toString();
    }
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
