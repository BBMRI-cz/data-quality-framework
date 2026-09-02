package eu.bbmri_eric.quality.server.dataquality.controller;

import eu.bbmri_eric.quality.server.dataquality.ManifestService;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestDTO;
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

  public ManifestController(ManifestService manifestService, ManifestLinkBuilder linkBuilder) {
    this.manifestService = manifestService;
    this.linkBuilder = linkBuilder;
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
}
