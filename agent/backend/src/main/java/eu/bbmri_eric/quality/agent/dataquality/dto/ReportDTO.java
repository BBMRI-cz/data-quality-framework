package eu.bbmri_eric.quality.agent.dataquality.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
public class ReportDTO {
  private List<QualityCheckResultDTO> results;

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
