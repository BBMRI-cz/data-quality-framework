package eu.bbmri_eric.quality.agent.dataquality.domain;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** A data quality check utilizing the Hl7 Clinical Quality Language queries for evaluation. */
@Entity(name = "quality_check")
@Getter
@Setter
public class QualityCheck implements DataQualityCheck {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String name;
  @NotBlank private String description;
  private String query;

  @Enumerated(EnumType.STRING)
  @Column(name = "check_type")
  @NotNull
  private QualityCheckType type = QualityCheckType.CQL;

  private int warningThreshold = 10;
  private int errorThreshold = 30;
  @Getter(AccessLevel.NONE)
  private Double epsilonBudget;

  @Override
  public Double getEpsilonBudget() {
    return epsilonBudget;
  }

  protected QualityCheck() {}

  public QualityCheck(
      Long id, @NotNull String name, @NotNull String description, @NotNull String query) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.query = query;
  }

  public QualityCheck(String name, String description, String query) {
    this.name = name;
    this.description = description;
    this.query = query;
  }

  @Override
  public ResultDTO execute(DataStore dataStore) {
    try {
      ResultDTO result = dataStore.executeQuery(query);
      return result != null
          ? result
          : new ResultDTO("No result returned from data store execution.");
    } catch (Exception | NoSuchMethodError e) {
      return new ResultDTO(e.getMessage());
    }
  }
}
