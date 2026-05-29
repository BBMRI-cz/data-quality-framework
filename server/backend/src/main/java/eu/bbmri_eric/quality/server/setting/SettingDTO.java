package eu.bbmri_eric.quality.server.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Settings Data Transfer Object")
public class SettingDTO {

  @Schema(description = "Maximum number of reports to retain per agent", example = "3")
  @Min(1)
  private Integer reportRetention;

  public SettingDTO() {}

  public Integer getReportRetention() {
    return reportRetention;
  }

  public void setReportRetention(Integer reportRetention) {
    this.reportRetention = reportRetention;
  }
}
