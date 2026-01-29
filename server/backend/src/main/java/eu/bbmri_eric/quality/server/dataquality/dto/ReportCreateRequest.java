package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;

/** Request DTO for creating a new report. */
@Schema(name = "Report Create Request", description = "Request object for creating a new report")
public final class ReportCreateRequest {
  @Schema(
      description = "List of quality check results",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Valid
  @NotEmpty(message = "Results cannot be empty")
  private List<QualityCheckResultDTO> results;

  @Schema(description = "Total number of patients", requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer totalPatients;

  @Schema(description = "Total number of samples", requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer totalSamples;

  public ReportCreateRequest() {}

  /**
   * Constructs a new {@code ReportCreateRequest} with the specified list of quality check results.
   *
   * @param results the list of quality check results to include in the report; must not be null
   * @throws NullPointerException if {@code results} is null
   */
  public ReportCreateRequest(List<QualityCheckResultDTO> results) {
    this(results, 0, 0);
  }

  /**
   * Constructs a new {@code ReportCreateRequest} with the specified list of quality check results.
   *
   * @param results the list of quality check results to include in the report; must not be null
   * @param totalPatients the total number of patients
   * @param totalSamples the total number of samples
   * @throws NullPointerException if {@code results} is null
   */
  public ReportCreateRequest(
      List<QualityCheckResultDTO> results, Integer totalPatients, Integer totalSamples) {
    Objects.requireNonNull(results);
    this.results = results;
    this.totalPatients = totalPatients;
    this.totalSamples = totalSamples;
  }

  public List<QualityCheckResultDTO> getResults() {
    return results;
  }

  public Integer getTotalPatients() {
    return totalPatients;
  }

  public Integer getTotalSamples() {
    return totalSamples;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ReportCreateRequest) obj;
    return Objects.equals(this.results, that.results)
        && Objects.equals(this.totalPatients, that.totalPatients)
        && Objects.equals(this.totalSamples, that.totalSamples);
  }

  @Override
  public int hashCode() {
    return Objects.hash(results, totalPatients, totalSamples);
  }

  @Override
  public String toString() {
    return "ReportCreateRequest["
        + "results="
        + results
        + ", totalPatients="
        + totalPatients
        + ", totalSamples="
        + totalSamples
        + ']';
  }
}
