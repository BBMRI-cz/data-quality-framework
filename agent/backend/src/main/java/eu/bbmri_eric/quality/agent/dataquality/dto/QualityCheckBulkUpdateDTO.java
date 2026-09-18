package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** DTO for a single quality check within a bulk update request. */
@Setter
@Getter
@Schema(
    name = "Quality Check Bulk Update",
    description = "Update data for one quality check within a bulk update request")
public class QualityCheckBulkUpdateDTO extends QualityCheckUpdateDTO {

  @NotNull(message = "ID is required for bulk update")
  @Schema(
      description = "ID of the quality check to update",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;
}
