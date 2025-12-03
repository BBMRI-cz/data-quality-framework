package eu.bbmri_eric.quality.agent.dataquality.dto;

import java.util.List;
import java.util.Objects;

public class ReportDTO {
  private List<QualityCheckResultDTO> results;

  public ReportDTO(List<QualityCheckResultDTO> results) {
    this.results = results;
  }

  public List<QualityCheckResultDTO> getResults() {
    return results;
  }

  public void setResults(List<QualityCheckResultDTO> results) {
    this.results = results;
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
