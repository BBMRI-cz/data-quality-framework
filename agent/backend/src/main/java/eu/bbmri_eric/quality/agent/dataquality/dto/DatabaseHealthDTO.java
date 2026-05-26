package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(name = "Database Health", description = "Health status information for the database")
public class DatabaseHealthDTO {
  @Schema(
      description = "Overall health status of the database",
      example = "UP",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public DBStatus status;

  @Schema(
      description = "Error message if the database is unhealthy",
      example = "Connection refused",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String error;

  @Schema(
      description = "Additional details about the health status",
      example = "Database connection pool exhausted",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String details;
}
