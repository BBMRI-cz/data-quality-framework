package eu.bbmri_eric.quality.agent.dataquality.controller;

import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "API for managing Data Quality reports")
class ReportController {

  private final ReportService reportService;
  private final ReportLinkBuilder linkBuilder;

  ReportController(ReportService reportService, ReportLinkBuilder linkBuilder) {
    this.reportService = reportService;
    this.linkBuilder = linkBuilder;
  }

  @PostMapping
  @Operation(summary = "Create a new report", description = "Creates a new data quality report")
  public ResponseEntity<EntityModel<ReportDTO>> create(
      @RequestBody @Valid ReportCreateDTO createDTO) {
    ReportDTO created = reportService.create(createDTO);
    EntityModel<ReportDTO> model = linkBuilder.toModel(created);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(location).body(model);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get report by ID", description = "Retrieves a report by its ID")
  public ResponseEntity<EntityModel<ReportDTO>> findById(@PathVariable Long id) {
    ReportDTO report = reportService.findById(id);
    return ResponseEntity.ok(linkBuilder.toModel(report));
  }

  @GetMapping
  @Operation(
      summary = "List all reports",
      description = "Retrieves all reports with pagination and filtering")
  public ResponseEntity<PagedModel<EntityModel<ReportDTO>>> findAll(
      @ParameterObject FilterDTO filter) {
    return ResponseEntity.ok(linkBuilder.toPagedModel(reportService.findAll(filter), filter));
  }
}
