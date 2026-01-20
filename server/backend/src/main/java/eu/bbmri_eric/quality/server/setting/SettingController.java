package eu.bbmri_eric.quality.server.setting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "Settings Management", description = "APIs for managing application settings")
public class SettingController {

  private final SettingService settingService;

  public SettingController(SettingService settingService) {
    this.settingService = settingService;
  }

  @Operation(summary = "Get all settings", description = "Retrieves application settings.")
  @GetMapping
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<SettingDTO> getSettings() {
    SettingDTO settings = settingService.getSettings();
    return ResponseEntity.ok(settings);
  }

  @Operation(
      summary = "Get OIDC settings",
      description = "Retrieves OIDC configuration for frontend initialization.")
  @GetMapping("/oidc")
  public ResponseEntity<OidcSettingsDTO> getOidcSettings() {
    OidcSettingsDTO oidcSettings = settingService.getOidcSettings();
    return ResponseEntity.ok(oidcSettings);
  }

  @Operation(
      summary = "Update OIDC settings",
      description =
          "Updates OIDC configuration only. Frontend should reload after OIDC settings change.")
  @PatchMapping("/oidc")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<OidcSettingsDTO> updateOidcSettings(
      @Valid @RequestBody OidcSettingsDTO oidcSettingsDTO) {
    OidcSettingsDTO updatedSettings = settingService.updateOidcSettings(oidcSettingsDTO);
    return ResponseEntity.ok(updatedSettings);
  }

  @Operation(
      summary = "Update settings",
      description =
          "Updates application settings. Frontend should reload after OIDC settings change.")
  @PatchMapping
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<SettingDTO> updateSettings(SettingDTO settingDTO) {
    SettingDTO updatedSettings = settingService.updateSettings(settingDTO);
    return ResponseEntity.ok(updatedSettings);
  }
}
