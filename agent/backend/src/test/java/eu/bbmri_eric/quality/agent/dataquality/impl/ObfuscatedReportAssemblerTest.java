package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.impl.SettingsRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Transactional
class ObfuscatedReportAssemblerTest {

  @Autowired private ObfuscatedReportAssembler assembler;
  @Autowired private SettingsRepository settingsRepository;
  @Autowired private ReportRepository reportRepository;
  @Autowired private BuildProperties buildProperties;
  @Autowired private JsonMapper jsonMapper;

  @BeforeEach
  void setUp() {
    reportRepository.deleteAll();
    useSettings("LAPLACE", "1.0", "1e-8", "10");
  }

  private void useSettings(String mechanism, String epsilon, String delta, String minThreshold) {
    settingsRepository.save(new Settings("noiseMechanism", mechanism));
    settingsRepository.save(new Settings("epsilon", epsilon));
    settingsRepository.save(new Settings("delta", delta));
    settingsRepository.save(new Settings("minThreshold", minThreshold));
  }

  @Test
  void assemble_shouldIncludeAgentVersionFromBuildProperties() {
    ObfuscatedReportDTO dto = assembler.assemble(new Report());

    assertThat(dto.getAgentVersion()).isEqualTo(buildProperties.getVersion());
  }

  @Test
  void assemble_shouldIncludePrivacyMetadataFromSettings() {
    ObfuscatedReportDTO dto = assembler.assemble(new Report());

    assertThat(dto.getPrivacy())
        .extracting(
            "mechanism",
            "epsilon",
            "delta",
            "minThreshold",
            "checkResultsEpsilon",
            "entityTotalEpsilon",
            "entityTotalMechanism")
        .containsExactly("LAPLACE", 1.0, 1e-8, 10, 0.8, 0.1, "LAPLACE");
  }

  @Test
  void assemble_shouldIncludeGaussianMechanismButLaplaceForEntityTotals() {
    useSettings("GAUSSIAN", "1.0", "1e-8", "10");

    ObfuscatedReportDTO dto = assembler.assemble(new Report());

    assertThat(dto.getPrivacy().getMechanism()).isEqualTo("GAUSSIAN");
    assertThat(dto.getPrivacy().getEntityTotalMechanism()).isEqualTo("LAPLACE");
  }

  @Test
  void assemble_shouldIncludePerResultEpsilonAndSensitivity() {
    Report report = new Report();
    report.setResults(
        new ArrayList<>(List.of(new Result("Check A", 1L, 100, 90.0, 10, 30, 0.8, null, null))));
    report.setObfuscatedNumberOfEntities(100);

    ObfuscatedReportDTO dto = assembler.assemble(report);

    assertThat(dto.getResults()).hasSize(1);
    assertThat(dto.getResults().getFirst().getEpsilon()).isEqualTo(0.8);
    assertThat(dto.getResults().getFirst().getSensitivity()).isEqualTo(1);
  }

  @Test
  void assemble_shouldIncludeTotalEpsilonAndSensitivityWhenTotalsPresent() {
    Report report = new Report();
    report.setObfuscatedNumberOfEntities(100);
    report.setObfuscatedNumberOfSecondaryEntities(250);

    ObfuscatedReportDTO dto = assembler.assemble(report);

    assertThat(dto.getTotalPatientsEpsilon()).isEqualTo(0.1);
    assertThat(dto.getTotalPatientsSensitivity()).isEqualTo(1);
    assertThat(dto.getTotalSamplesEpsilon()).isEqualTo(0.1);
    assertThat(dto.getTotalSamplesSensitivity()).isEqualTo(20);
  }

  @Test
  void assemble_shouldSerializeMetadataWithExactFieldNames() {
    Report report = new Report();
    report.setResults(
        new ArrayList<>(List.of(new Result("Check A", 1L, 100, 90.0, 10, 30, 0.8, null, null))));
    report.setObfuscatedNumberOfEntities(100);
    report.setObfuscatedNumberOfSecondaryEntities(250);

    JsonNode root = jsonMapper.readTree(jsonMapper.writeValueAsString(assembler.assemble(report)));

    assertThat(root.path("agentVersion").asText()).isEqualTo(buildProperties.getVersion());
    JsonNode privacy = root.path("privacy");
    assertThat(privacy.path("mechanism").asText()).isEqualTo("LAPLACE");
    assertThat(privacy.path("epsilon").asDouble()).isEqualTo(1.0);
    assertThat(privacy.path("delta").asDouble()).isEqualTo(1e-8);
    assertThat(privacy.path("minThreshold").asInt()).isEqualTo(10);
    assertThat(privacy.path("checkResultsEpsilon").asDouble()).isEqualTo(0.8);
    assertThat(privacy.path("entityTotalEpsilon").asDouble()).isEqualTo(0.1);
    assertThat(privacy.path("entityTotalMechanism").asText()).isEqualTo("LAPLACE");
    JsonNode result = root.path("results").get(0);
    assertThat(result.path("epsilon").asDouble()).isEqualTo(0.8);
    assertThat(result.path("sensitivity").asInt()).isEqualTo(1);
    assertThat(root.path("totalPatientsEpsilon").asDouble()).isEqualTo(0.1);
    assertThat(root.path("totalPatientsSensitivity").asInt()).isEqualTo(1);
    assertThat(root.path("totalSamplesEpsilon").asDouble()).isEqualTo(0.1);
    assertThat(root.path("totalSamplesSensitivity").asInt()).isEqualTo(20);
  }

  @Test
  void assemble_shouldOmitMetadataWhenAbsent() {
    Report report = new Report();
    report.setResults(
        new ArrayList<>(List.of(new Result("Check A", 1L, null, null, 10, 30, null, null, null))));

    JsonNode root = jsonMapper.readTree(jsonMapper.writeValueAsString(assembler.assemble(report)));

    assertThat(root.has("totalPatientsEpsilon")).isFalse();
    assertThat(root.has("totalPatientsSensitivity")).isFalse();
    assertThat(root.has("totalSamplesEpsilon")).isFalse();
    assertThat(root.has("totalSamplesSensitivity")).isFalse();
    assertThat(root.path("results").get(0).has("epsilon")).isFalse();
  }
}
