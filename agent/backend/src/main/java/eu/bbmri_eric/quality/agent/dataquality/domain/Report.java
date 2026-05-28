package eu.bbmri_eric.quality.agent.dataquality.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a data quality report.
 *
 * <p>This entity encapsulates the results of a data quality assessment, including the timestamp of
 * generation, current status, and a collection of individual quality check results. Each report
 * tracks the epsilon budget used for differential privacy operations and the number of entities
 * assessed.
 */
@Entity
@Getter
@Setter
public class Report {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime generatedAt;

  @Enumerated(EnumType.STRING)
  private ReportStatus status = ReportStatus.GENERATING;

  private double epsilonBudget = 2.0;

  private Integer numberOfEntities;

  private Integer numberOfSecondaryEntities;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "report_id")
  private List<Result> results = new ArrayList<>();

  public Report() {}

  public void addResult(Result result) {
    results.add(result);
  }

  @PrePersist
  protected void onCreate() {
    generatedAt = LocalDateTime.now();
  }
}
