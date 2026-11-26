package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** Outcome of a quality check import: how many checks were created, updated or left unchanged. */
@Getter
@Schema(
    name = "Quality Check Import Summary",
    description = "Counts of quality checks created, updated or left unchanged by an import")
public class QualityCheckImportSummaryDTO {

  @Schema(description = "Number of checks created", example = "2")
  private final int created;

  @Schema(description = "Number of existing checks updated", example = "1")
  private final int updated;

  @Schema(description = "Number of existing checks that needed no change", example = "3")
  private final int unchanged;

  public QualityCheckImportSummaryDTO(int created, int updated, int unchanged) {
    this.created = created;
    this.updated = updated;
    this.unchanged = unchanged;
  }
}
