package eu.bbmri_eric.quality.server.dataquality.controller;

import eu.bbmri_eric.quality.server.dataquality.ManifestService;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestVersionCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestVersionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for managing quality check manifests. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Manifests", description = "API for managing signed quality check manifests")
class ManifestController {

  private final ManifestService manifestService;
  private final ManifestLinkBuilder linkBuilder;
  private final ManifestVersionLinkBuilder versionLinkBuilder;

  public ManifestController(
      ManifestService manifestService,
      ManifestLinkBuilder linkBuilder,
      ManifestVersionLinkBuilder versionLinkBuilder) {
    this.manifestService = manifestService;
    this.linkBuilder = linkBuilder;
    this.versionLinkBuilder = versionLinkBuilder;
  }

  @GetMapping("/manifests/{id}")
  @Operation(
      summary = "Get manifest by ID",
      description = "Retrieves a specific manifest by its unique identifier (id)")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<ManifestDTO>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(linkBuilder.toModel(manifestService.findById(id)));
  }

  @GetMapping("/manifests")
  @Operation(summary = "Get all manifests", description = "Retrieves all quality check manifests")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CollectionModel<EntityModel<ManifestDTO>>> findAll() {
    List<ManifestDTO> manifests = manifestService.findAll();
    CollectionModel<EntityModel<ManifestDTO>> manifestsModel =
        linkBuilder.toCollectionModel(manifests);
    return ResponseEntity.ok(manifestsModel);
  }

  @PostMapping("/manifests")
  @Operation(summary = "Create manifest", description = "Creates a new quality check manifest")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<ManifestDTO>> create(
      @Valid @RequestBody ManifestCreateDTO createDTO) {
    ManifestDTO createdManifest = manifestService.create(createDTO);
    EntityModel<ManifestDTO> manifestModel = linkBuilder.toModel(createdManifest);
    return ResponseEntity.status(HttpStatus.CREATED).body(manifestModel);
  }

  @PostMapping("/manifests/{id}/versions")
  @Operation(
      summary = "Publish manifest version",
      description = "Publishes a new signed version of a manifest")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<ManifestVersionDTO>> createVersion(
      @PathVariable Long id, @Valid @RequestBody ManifestVersionCreateDTO createDTO) {
    ManifestVersionDTO createdVersion = manifestService.createVersion(id, createDTO);
    EntityModel<ManifestVersionDTO> versionModel = versionLinkBuilder.toModel(id, createdVersion);
    return ResponseEntity.status(HttpStatus.CREATED).body(versionModel);
  }

  @GetMapping("/manifests/{id}/versions")
  @Operation(
      summary = "Get manifest versions",
      description = "Retrieves all versions of a manifest")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CollectionModel<EntityModel<ManifestVersionDTO>>> findVersions(
      @PathVariable Long id) {
    List<ManifestVersionDTO> versions = manifestService.findVersions(id);
    CollectionModel<EntityModel<ManifestVersionDTO>> versionsModel =
        versionLinkBuilder.toCollectionModel(id, versions);
    return ResponseEntity.ok(versionsModel);
  }
}
