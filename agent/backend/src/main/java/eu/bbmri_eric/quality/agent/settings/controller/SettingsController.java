package eu.bbmri_eric.quality.agent.settings.controller;

import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "Application settings management")
@SecurityRequirement(name = "bearerAuth")
class SettingsController {

  private final SettingsService settingsService;

  @Autowired
  public SettingsController(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @GetMapping
  @Operation(
      summary = "Get application settings",
      description =
          "Retrieve current application configuration including FHIR server and differential privacy settings")
  public SettingsDTO getSettings() {
    return settingsService.getSettings();
  }

  @PutMapping
  @Operation(
      summary = "Update application settings",
      description =
          "Update application configuration including FHIR server and differential privacy settings")
  public SettingsDTO updateSettings(@Valid @RequestBody SettingsDTO settingsDTO) {
    return settingsService.updateSettings(settingsDTO);
  }
}
