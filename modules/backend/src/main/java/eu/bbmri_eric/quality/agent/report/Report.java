package eu.bbmri_eric.quality.agent.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
class Report {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime generatedAt;

  private Status status = Status.GENERATING;

  private float epsilonBudget = 2.0f;

  private int numberOfEntities;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "report_id")
  private List<Result> results = new ArrayList<>();

  protected Report() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void addResult(Result result) {
    results.add(result);
  }

  public List<Result> getResults() {
    return results;
  }

  public float getEpsilonBudget() {
    return epsilonBudget;
  }

  public void setEpsilonBudget(float epsilonBudget) {
    this.epsilonBudget = epsilonBudget;
  }

  public int getNumberOfEntities() {
    return numberOfEntities;
  }

  public void setNumberOfEntities(int numberOfEntities) {
    this.numberOfEntities = numberOfEntities;
  }

  @PrePersist
  protected void onCreate() {
    generatedAt = LocalDateTime.now();
  }
}
