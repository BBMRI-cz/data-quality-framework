package eu.bbmri_eric.quality.agent.logs.controller;

import eu.bbmri_eric.quality.agent.logs.LogService;
import eu.bbmri_eric.quality.agent.logs.dto.LogEntryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing the most recent application logs retained in memory. */
@RestController
@Tag(name = "Logs", description = "API for inspecting recent application logs")
@SecurityRequirement(name = "bearerAuth")
class LogController {

  private final LogService logService;

  LogController(LogService logService) {
    this.logService = logService;
  }

  @Operation(
      summary = "Get recent logs",
      description = "Returns the most recent log entries captured by the in-memory log appender")
  @GetMapping("/api/logs")
  public List<LogEntryDTO> getRecentLogs() {
    return logService.getRecentLogs();
  }
}
