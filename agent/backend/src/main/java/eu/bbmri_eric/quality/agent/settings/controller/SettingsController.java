package eu.bbmri_eric.quality.agent.settings.controller;

import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "FHIR server settings management")
class SettingsController {

  private final SettingsService settingsService;

  @Autowired
  public SettingsController(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @GetMapping
  @Operation(
      summary = "Get FHIR server settings",
      description = "Retrieve current FHIR server configuration")
  public SettingsDTO getSettings() {
    return settingsService.getSettings();
  }

  @PutMapping
  @Operation(
      summary = "Update FHIR server settings",
      description = "Update FHIR server configuration")
  public SettingsDTO updateSettings(@Valid @RequestBody SettingsDTO settingsDto) {
    return settingsService.updateSettings(settingsDto);
  }
}
