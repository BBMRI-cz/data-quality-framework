package eu.bbmri_eric.quality.agent.dataquality.dto;

import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ReportDTO {
  private Long id;
  private LocalDateTime generatedAt;
  private ReportStatus status;
  private float epsilonBudget;
  private int numberOfEntities;
  private int numberOfSecondaryEntities;
  private List<ReportResultDTO> results;
}
