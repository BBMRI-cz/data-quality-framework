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
  private final List<QualityCheckResultDTO> results;

  /**
   * Constructs a new {@code ReportCreateRequest} with the specified list of quality check results.
   *
   * @param results the list of quality check results to include in the report; must not be null
   * @throws NullPointerException if {@code results} is null
   */
  public ReportCreateRequest(List<QualityCheckResultDTO> results) {
    Objects.requireNonNull(results);
    this.results = results;
  }

  public List<QualityCheckResultDTO> getResults() {
    return results;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ReportCreateRequest) obj;
    return Objects.equals(this.results, that.results);
  }

  @Override
  public int hashCode() {
    return Objects.hash(results);
  }

  @Override
  public String toString() {
    return "ReportCreateRequest[" + "results=" + results + ']';
  }
}
