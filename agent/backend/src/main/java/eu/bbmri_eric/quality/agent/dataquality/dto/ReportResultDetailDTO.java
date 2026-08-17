package eu.bbmri_eric.quality.agent.dataquality.dto;

import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReportResultDetailDTO extends ReportResultDTO {
  private Set<String> patients;
}
