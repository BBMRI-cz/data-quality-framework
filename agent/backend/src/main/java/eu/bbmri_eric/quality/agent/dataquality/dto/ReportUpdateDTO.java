package eu.bbmri_eric.quality.agent.dataquality.dto;

import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import lombok.Data;

@Data
public class ReportUpdateDTO {
  private ReportStatus status;
}
