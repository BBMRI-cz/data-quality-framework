package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportDTO {

  private List<QualityCheckResultDTO> results;

  @Schema(description = "Total number of patients", example = "100")
  private Integer totalPatients;

  @Schema(description = "Total number of samples", example = "250")
  private Integer totalSamples;

  public ReportDTO(List<QualityCheckResultDTO> results) {
    this.results = results;
  }

  public ReportDTO(
      List<QualityCheckResultDTO> results, Integer totalPatients, Integer totalSamples) {
    this.results = results;
    this.totalPatients = totalPatients;
    this.totalSamples = totalSamples;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ReportDTO reportDTO = (ReportDTO) o;
    return Objects.equals(results, reportDTO.results);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(results);
  }
}
