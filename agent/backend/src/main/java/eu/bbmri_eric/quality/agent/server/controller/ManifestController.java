package eu.bbmri_eric.quality.agent.server.controller;

import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.ManifestService;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API for browsing quality check manifests published by registered central servers. These endpoints
 * proxy live requests to the central server; no manifest data is persisted on the agent by calling
 * them.
 */
@RestController
@RequestMapping("/api/servers/{serverId}/manifests")
@Tag(
    name = "Manifests",
    description = "API for browsing manifests published by registered central servers")
@SecurityRequirement(name = "bearerAuth")
class ManifestController {

  private final ManifestService manifestService;

  ManifestController(ManifestService manifestService) {
    this.manifestService = manifestService;
  }

  @GetMapping
  @Operation(
      summary = "List server manifests",
      description =
          "Fetches all manifests published by the given central server without downloading them")
  public ResponseEntity<List<ManifestDto>> findAll(
      @Parameter(description = "Server ID", required = true) @PathVariable String serverId) {
    return ResponseEntity.ok(manifestService.fetchManifests(serverId));
  }

  @GetMapping("/{manifestId}")
  @Operation(
      summary = "Get server manifest",
      description =
          "Fetches a manifest including its signed versions from the given central server without downloading it")
  public ResponseEntity<ManifestDto> findById(
      @Parameter(description = "Server ID", required = true) @PathVariable String serverId,
      @Parameter(description = "Manifest ID on the central server", required = true) @PathVariable
          Long manifestId) {
    return ResponseEntity.ok(manifestService.fetchManifest(serverId, manifestId));
  }

  @GetMapping("/{manifestId}/versions/{versionId}/quality-checks")
  @Operation(
      summary = "Get quality checks of a manifest version",
      description =
          "Fetches the quality checks pinned by the given manifest version from the central server without persisting them")
  public ResponseEntity<List<QualityCheckDTO>> findVersionQualityChecks(
      @Parameter(description = "Server ID", required = true) @PathVariable String serverId,
      @Parameter(description = "Manifest ID on the central server", required = true) @PathVariable
          Long manifestId,
      @Parameter(description = "Manifest version ID on the central server", required = true)
          @PathVariable
          Long versionId) {
    return ResponseEntity.ok(
        manifestService.fetchVersionQualityChecks(serverId, manifestId, versionId));
  }
}
