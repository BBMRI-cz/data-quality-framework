package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.event.ReportGeneratedEvent;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

@Component
class ReportSender {

  private static final Logger log = LoggerFactory.getLogger(ReportSender.class);
  private final ReportService reportService;
  private final CentralServerClientFactory clientFactory;
  private final ServerRepository serverRepository;
  private final SettingsService settingsService;
  private final JsonMapper objectMapper;

  ReportSender(
      ReportService reportService,
      CentralServerClientFactory clientFactory,
      ServerRepository serverRepository,
      SettingsService settingsService,
      JsonMapper objectMapper) {
    this.reportService = reportService;
    this.clientFactory = clientFactory;
    this.serverRepository = serverRepository;
    this.settingsService = settingsService;
    this.objectMapper = objectMapper;
  }

  /**
   * Sends a newly generated report to all active central servers. Runs only after the report
   * pipeline transaction committed, so a report whose transaction rolled back is never sent and the
   * server can never hold values the agent did not record. {@code @Async} runs the sends on a
   * separate thread without a bound transaction: a failure here cannot affect the (already
   * committed) report status, and no database connection is held during the HTTP calls.
   *
   * @param event the report generated event
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  protected void onFinished(ReportGeneratedEvent event) {
    ObfuscatedReportDTO reportDTO = reportService.getObfuscatedById(event.getReportId());
    String agentId = settingsService.getSettings().getAgentId();
    serverRepository
        .findAllByStatusIs(ServerConnectionStatus.ACTIVE)
        .forEach(
            server -> {
              try {
                var client =
                    clientFactory.createClient(
                        agentId, server.getUrl(), server.getClientId(), server.getClientSecret());
                client.sendReport(reportDTO);
                String reportJson = objectMapper.writeValueAsString(reportDTO);
                serverRepository
                    .findWithInteractionsByUrl(server.getUrl())
                    .ifPresent(
                        reloaded -> {
                          reloaded.addInteraction(
                              new ServerInteraction(InteractionType.COMMUNICATION, reportJson));
                          serverRepository.save(reloaded);
                        });
              } catch (Exception e) {
                log.error("Failed to send report to server {}", server.getUrl(), e);
              }
            });
  }
}
