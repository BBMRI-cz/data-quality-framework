package eu.bbmri_eric.quality.agent.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObfuscatedReportDTO {

  private List<QualityCheckResultDTO> results;

  @Schema(description = "Total number of patients", example = "100")
  private Integer totalPatients;

  @Schema(description = "Total number of samples", example = "250")
  private Integer totalSamples;

  @Schema(
      description = "Version of the agent that produced this report",
      example = "0.7.0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String agentVersion;

  @Schema(
      description = "Privacy parameters that produced this report",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private PrivacyMetadataDTO privacy;

  @Schema(
      description = "Epsilon spent on the released patient total",
      example = "0.1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Double totalPatientsEpsilon;

  @Schema(
      description = "Sensitivity of the patient total query",
      example = "1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer totalPatientsSensitivity;

  @Schema(
      description = "Epsilon spent on the released sample total",
      example = "0.1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Double totalSamplesEpsilon;

  @Schema(
      description = "Sensitivity of the sample total query",
      example = "20",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer totalSamplesSensitivity;

  public ObfuscatedReportDTO(List<QualityCheckResultDTO> results) {
    this.results = results;
  }

  public ObfuscatedReportDTO(
      List<QualityCheckResultDTO> results, Integer totalPatients, Integer totalSamples) {
    this.results = results;
    this.totalPatients = totalPatients;
    this.totalSamples = totalSamples;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ObfuscatedReportDTO reportDTO = (ObfuscatedReportDTO) o;
    return Objects.equals(results, reportDTO.results);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(results);
  }
}
