package eu.bbmri_eric.quality.agent.audit.controller;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.AuditService;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "API for querying audit log entries")
@SecurityRequirement(name = "bearerAuth")
class AuditController {

  private final AuditService auditService;
  private final AuditLogLinkBuilder linkBuilder;

  AuditController(AuditService auditService, AuditLogLinkBuilder linkBuilder) {
    this.auditService = auditService;
    this.linkBuilder = linkBuilder;
  }

  @GetMapping
  @Operation(
      summary = "List audit log entries",
      description =
          "Retrieves audit log entries with pagination, sorting, filtering, and free-text search")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "List of audit log entries retrieved")
      })
  public ResponseEntity<PagedModel<EntityModel<AuditLogDTO>>> findAll(
      @ParameterObject AuditLogFilterDTO filter) {
    return ResponseEntity.ok(linkBuilder.toPagedModel(auditService.findAll(filter), filter));
  }

  @GetMapping(value = "/export", produces = "text/csv")
  @Operation(
      summary = "Export audit log entries as CSV",
      description =
          "Downloads every audit log entry matching the filters as a CSV file, newest first."
              + " Pagination parameters are ignored.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV file with the matching audit log entries",
            content = @Content(mediaType = "text/csv"))
      })
  public void exportCsv(@ParameterObject AuditLogFilterDTO filter, HttpServletResponse response)
      throws IOException {
    response.setContentType("text/csv");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment()
            .filename("audit-log-" + LocalDate.now() + ".csv")
            .build()
            .toString());
    auditService.exportCsv(filter, response.getWriter());
  }

  @GetMapping("/actions")
  @Operation(
      summary = "List audit actions",
      description = "Retrieves every audit action an entry can have, e.g. for filtering")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "List of audit actions retrieved")})
  public ResponseEntity<List<AuditAction>> findActions() {
    return ResponseEntity.ok(List.of(AuditAction.values()));
  }
}
