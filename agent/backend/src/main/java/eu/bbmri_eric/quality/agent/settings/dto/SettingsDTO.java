package eu.bbmri_eric.quality.agent.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Settings Data Transfer Object")
public class SettingsDTO {

  @Schema(
      description = "Agent identifier",
      example = "agent-12345",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String agentId;

  public SettingsDTO() {}
}
