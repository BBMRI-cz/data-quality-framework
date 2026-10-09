package eu.bbmri_eric.quality.server.config.controller;

import eu.bbmri_eric.quality.server.common.CountsDTO;
import eu.bbmri_eric.quality.server.dataquality.AgentService;
import eu.bbmri_eric.quality.server.dataquality.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for application information endpoints. */
@RestController
@RequestMapping("/api")
@Tag(name = "Info", description = "API for application information")
public class InfoController {

  private final AgentService agentService;
  private final ReportService reportService;

  public InfoController(AgentService agentService, ReportService reportService) {
    this.agentService = agentService;
    this.reportService = reportService;
  }

  @GetMapping("/counts")
  @Operation(
      summary = "Get system counts",
      description = "Returns counts of agents and reports in the system")
  @SecurityRequirements
  public ResponseEntity<CountsDTO> getCounts() {
    long agentCount = agentService.countAll();
    long reportCount = reportService.countAll();
    CountsDTO counts = new CountsDTO(agentCount, reportCount);
    return ResponseEntity.ok(counts);
  }
}
