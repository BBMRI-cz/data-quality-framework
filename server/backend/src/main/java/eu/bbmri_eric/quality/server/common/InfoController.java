package eu.bbmri_eric.quality.server.common;

import eu.bbmri_eric.quality.server.dataquality.AgentService;
import eu.bbmri_eric.quality.server.dataquality.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for application information endpoints. */
@RestController
@RequestMapping("/api")
@Tag(name = "Info", description = "API for application information")
public class InfoController {

  private final BuildProperties buildProperties;
  private final AgentService agentService;
  private final ReportService reportService;

  public InfoController(
      BuildProperties buildProperties, AgentService agentService, ReportService reportService) {
    this.buildProperties = buildProperties;
    this.agentService = agentService;
    this.reportService = reportService;
  }

  @GetMapping("/counts")
  @Operation(
      summary = "Get system counts",
      description = "Returns counts of agents and reports in the system")
  public ResponseEntity<CountsDTO> getCounts() {
    long agentCount = agentService.countAll();
    long reportCount = reportService.countAll();
    CountsDTO counts = new CountsDTO(agentCount, reportCount);
    return ResponseEntity.ok(counts);
  }
}
