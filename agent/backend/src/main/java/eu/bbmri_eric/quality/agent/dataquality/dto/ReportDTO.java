package eu.bbmri_eric.quality.agent.dataquality.dto;

import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.hateoas.server.core.Relation;

@Data
@Relation(itemRelation = "report", collectionRelation = "reports")
public class ReportDTO {
  private Long id;
  private LocalDateTime generatedAt;
  private ReportStatus status;
  private double epsilonBudget;
  private Integer numberOfEntities;
  private Integer numberOfSecondaryEntities;
  private List<ReportResultDTO> results;
}
