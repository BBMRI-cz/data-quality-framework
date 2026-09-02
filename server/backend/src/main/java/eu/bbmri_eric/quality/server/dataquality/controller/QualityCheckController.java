package eu.bbmri_eric.quality.server.dataquality.controller;

import eu.bbmri_eric.quality.server.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.server.dataquality.dto.KeywordsDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDetailedDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionDTO;
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

/** REST controller for managing quality checks. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Quality Checks", description = "API for managing quality check definitions")
class QualityCheckController {

  private final QualityCheckService qualityCheckService;
  private final QualityCheckLinkBuilder linkBuilder;
  private final QualityCheckVersionLinkBuilder versionLinkBuilder;

  public QualityCheckController(
      QualityCheckService qualityCheckService,
      QualityCheckLinkBuilder linkBuilder,
      QualityCheckVersionLinkBuilder versionLinkBuilder) {
    this.qualityCheckService = qualityCheckService;
    this.linkBuilder = linkBuilder;
    this.versionLinkBuilder = versionLinkBuilder;
  }

  @GetMapping("/quality-checks/{id}")
  @Operation(
      summary = "Get quality check by ID",
      description = "Retrieves a specific quality check by its unique identifier (id)")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<QualityCheckDetailedDTO>> findById(@PathVariable Long id) {
    QualityCheckDetailedDTO qualityCheck = qualityCheckService.findById(id);
    EntityModel<QualityCheckDetailedDTO> qualityCheckModel = linkBuilder.toModel(qualityCheck);
    return ResponseEntity.ok(qualityCheckModel);
  }

  @GetMapping("/quality-checks")
  @Operation(
      summary = "Get all quality checks",
      description = "Retrieves all quality check definitions, including their versions")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CollectionModel<EntityModel<QualityCheckDetailedDTO>>> findAll() {
    List<QualityCheckDetailedDTO> qualityChecks = qualityCheckService.findAll();
    CollectionModel<EntityModel<QualityCheckDetailedDTO>> qualityChecksModel =
        linkBuilder.toCollectionModel(qualityChecks);
    return ResponseEntity.ok(qualityChecksModel);
  }

  @PutMapping("/quality-checks/{id}")
  @Operation(
      summary = "Update quality check",
      description = "Updates an existing quality check definition")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<QualityCheckDTO>> update(
      @PathVariable Long id, @Valid @RequestBody QualityCheckUpdateDTO updateDTO) {
    QualityCheckDTO updatedQualityCheck = qualityCheckService.update(id, updateDTO);
    EntityModel<QualityCheckDTO> qualityCheckModel = linkBuilder.toModel(updatedQualityCheck);
    return ResponseEntity.ok(qualityCheckModel);
  }

  @PutMapping("/quality-checks/{id}/keywords")
  @Operation(
      summary = "Set keywords for quality check",
      description = "Sets the keywords for a quality check, replacing all existing keywords")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<QualityCheckDTO>> setKeywords(
      @PathVariable Long id, @Valid @RequestBody KeywordsDTO keywordsDTO) {
    QualityCheckDTO updatedQualityCheck =
        qualityCheckService.setKeywords(id, keywordsDTO.getKeywords());
    EntityModel<QualityCheckDTO> qualityCheckModel = linkBuilder.toModel(updatedQualityCheck);
    return ResponseEntity.ok(qualityCheckModel);
  }

  @PostMapping("/quality-checks/{id}/versions")
  @Operation(
      summary = "Create quality check version",
      description = "Creates a new immutable version of a quality check query")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<QualityCheckVersionDTO>> createVersion(
      @PathVariable Long id, @Valid @RequestBody QualityCheckVersionCreateDTO createDTO) {
    QualityCheckVersionDTO createdVersion = qualityCheckService.createVersion(id, createDTO);
    EntityModel<QualityCheckVersionDTO> versionModel =
        versionLinkBuilder.toModel(id, createdVersion);
    return ResponseEntity.status(HttpStatus.CREATED).body(versionModel);
  }

  @GetMapping("/quality-checks/{id}/versions")
  @Operation(
      summary = "Get quality check versions",
      description = "Retrieves all versions of a quality check")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CollectionModel<EntityModel<QualityCheckVersionDTO>>> findVersions(
      @PathVariable Long id) {
    List<QualityCheckVersionDTO> versions = qualityCheckService.findVersions(id);
    CollectionModel<EntityModel<QualityCheckVersionDTO>> versionsModel =
        versionLinkBuilder.toCollectionModel(id, versions);
    return ResponseEntity.ok(versionsModel);
  }
}
