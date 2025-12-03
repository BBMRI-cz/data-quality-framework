package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.ServerService;
import eu.bbmri_eric.quality.agent.server.dto.ServerCreateDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Auto-registers a central reporting server at application startup if configured via environment
 * variable.
 *
 * <p>This component checks for the REPORTING_SERVER_URL environment variable and automatically
 * registers the server if it's provided and not already registered.
 */
@Component
class AutoServerRegistration implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AutoServerRegistration.class);

  private final ServerService serverService;

  @Value("${reporting.server.url:}")
  private String reportingServerUrl;

  @Value("${reporting.server.name:Central Reporting Server}")
  private String reportingServerName;

  public AutoServerRegistration(ServerService serverService) {
    this.serverService = serverService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (reportingServerUrl == null || reportingServerUrl.trim().isEmpty()) {
      log.info(
          "No REPORTING_SERVER_URL environment variable provided. Skipping auto-registration.");
      return;
    }
    String trimmedUrl = reportingServerUrl.trim();
    log.info("REPORTING_SERVER_URL detected: {}", trimmedUrl);
    boolean alreadyRegistered =
        serverService.findAll().stream().anyMatch(server -> trimmedUrl.equals(server.getUrl()));

    if (alreadyRegistered) {
      log.info("Server already registered with URL: {}. Skipping auto-registration.", trimmedUrl);
      return;
    }

    try {
      ServerCreateDto createDto = new ServerCreateDto(trimmedUrl, reportingServerName);
      ServerDto createdServer = serverService.create(createDto);
      log.info(
          "Successfully auto-registered central reporting server: {} ({})",
          createdServer.getName(),
          createdServer.getUrl());
    } catch (Exception e) {
      log.error("Failed to auto-register central reporting server with URL: {}", trimmedUrl, e);
    }
  }
}
