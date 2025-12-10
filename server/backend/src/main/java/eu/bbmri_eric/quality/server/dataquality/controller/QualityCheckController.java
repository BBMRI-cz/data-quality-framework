package eu.bbmri_eric.quality.server.dataquality.controller;

import eu.bbmri_eric.quality.server.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for managing quality checks. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Quality Checks", description = "API for managing quality check definitions")
class QualityCheckController {

  private final QualityCheckService qualityCheckService;
  private final QualityCheckLinkBuilder linkBuilder;

  public QualityCheckController(
      QualityCheckService qualityCheckService, QualityCheckLinkBuilder linkBuilder) {
    this.qualityCheckService = qualityCheckService;
    this.linkBuilder = linkBuilder;
  }

  @GetMapping("/quality-checks/{id}")
  @Operation(
      summary = "Get quality check by ID",
      description = "Retrieves a specific quality check by its unique identifier (hash)")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<QualityCheckDTO>> findById(@PathVariable String id) {
    QualityCheckDTO qualityCheck = qualityCheckService.findById(id);
    EntityModel<QualityCheckDTO> qualityCheckModel = linkBuilder.toModel(qualityCheck);
    return ResponseEntity.ok(qualityCheckModel);
  }

  @GetMapping("/quality-checks")
  @Operation(
      summary = "Get all quality checks",
      description = "Retrieves all quality check definitions")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CollectionModel<EntityModel<QualityCheckDTO>>> findAll() {
    List<QualityCheckDTO> qualityChecks = qualityCheckService.findAll();
    CollectionModel<EntityModel<QualityCheckDTO>> qualityChecksModel =
        linkBuilder.toCollectionModel(qualityChecks);
    return ResponseEntity.ok(qualityChecksModel);
  }

  @PutMapping("/quality-checks/{id}")
  @Operation(
      summary = "Update quality check",
      description = "Updates an existing quality check definition")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<QualityCheckDTO>> update(
      @PathVariable String id, @Valid @RequestBody QualityCheckUpdateDTO updateDTO) {
    QualityCheckDTO updatedQualityCheck = qualityCheckService.update(id, updateDTO);
    EntityModel<QualityCheckDTO> qualityCheckModel = linkBuilder.toModel(updatedQualityCheck);
    return ResponseEntity.ok(qualityCheckModel);
  }
}
