package eu.bbmri_eric.quality.agent.dataquality.controller;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckFilterDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/quality-checks")
@Tag(name = "Quality Checks", description = "API for managing Data Quality checks")
@SecurityRequirement(name = "bearerAuth")
class QualityCheckController {

  private final QualityCheckService qualityCheckService;
  private final QualityCheckLinkBuilder linkBuilder;

  QualityCheckController(
      QualityCheckService qualityCheckService, QualityCheckLinkBuilder linkBuilder) {
    this.qualityCheckService = qualityCheckService;
    this.linkBuilder = linkBuilder;
  }

  @PostMapping
  @Operation(
      summary = "Create a new quality check",
      description = "Creates a new data quality check")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Quality check created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  public ResponseEntity<EntityModel<QualityCheckDTO>> create(
      @RequestBody @Valid QualityCheckCreateDTO createDTO) {
    QualityCheckDTO created = qualityCheckService.create(createDTO);
    EntityModel<QualityCheckDTO> model = linkBuilder.toModel(created);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(location).body(model);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get quality check by ID",
      description = "Retrieves a quality check by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Quality check found"),
        @ApiResponse(responseCode = "404", description = "Quality check not found")
      })
  public ResponseEntity<EntityModel<QualityCheckDTO>> findById(
      @Parameter(description = "Quality check ID") @PathVariable Long id) {
    QualityCheckDTO qualityCheck = qualityCheckService.findById(id);
    return ResponseEntity.ok(linkBuilder.toModel(qualityCheck));
  }

  @GetMapping
  @Operation(
      summary = "List all quality checks",
      description = "Retrieves all quality checks with pagination and filtering")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "List of quality checks retrieved")
      })
  public ResponseEntity<PagedModel<EntityModel<QualityCheckDTO>>> findAll(
      @ParameterObject QualityCheckFilterDTO filter) {
    return ResponseEntity.ok(linkBuilder.toPagedModel(qualityCheckService.findAll(filter), filter));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a quality check", description = "Updates an existing quality check")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Quality check updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Quality check not found")
      })
  public ResponseEntity<EntityModel<QualityCheckDTO>> update(
      @Parameter(description = "Quality check ID") @PathVariable Long id,
      @RequestBody @Valid QualityCheckUpdateDTO updateDTO) {
    QualityCheckDTO updated = qualityCheckService.update(id, updateDTO);
    return ResponseEntity.ok(linkBuilder.toModel(updated));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a quality check", description = "Deletes a quality check by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Quality check deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Quality check not found")
      })
  public ResponseEntity<Void> delete(
      @Parameter(description = "Quality check ID") @PathVariable Long id) {
    qualityCheckService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
