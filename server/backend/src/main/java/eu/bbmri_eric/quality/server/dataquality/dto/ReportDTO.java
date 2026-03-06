package eu.bbmri_eric.quality.server.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.springframework.hateoas.server.core.Relation;

/** Response DTO for report data. */
@Schema(description = "Report data transfer object")
@Relation(itemRelation = "report", collectionRelation = "reports")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportDTO {

  @Schema(
      description = "Unique identifier of the report",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private String id;

  @Schema(description = "Timestamp when the report was created", example = "2025-10-12T13:30:00")
  private Instant timestamp;

  @Schema(
      description = "ID of the agent that generated this report",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private String agentId;

  @Schema(description = "Total number of patients", example = "100")
  private Integer totalPatients;

  @Schema(description = "Total number of samples", example = "250")
  private Integer totalSamples;

  @Schema(description = "Quality check results for this report")
  private List<QualityCheckResultDTO> results;

  public ReportDTO() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public Integer getTotalPatients() {
    return totalPatients;
  }

  public void setTotalPatients(Integer totalPatients) {
    this.totalPatients = totalPatients;
  }

  public Integer getTotalSamples() {
    return totalSamples;
  }

  public void setTotalSamples(Integer totalSamples) {
    this.totalSamples = totalSamples;
  }

  public List<QualityCheckResultDTO> getResults() {
    return results;
  }

  public void setResults(List<QualityCheckResultDTO> results) {
    this.results = results;
  }
}
