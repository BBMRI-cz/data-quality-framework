package eu.bbmri_eric.quality.agent.server.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.CQLQueryService;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.event.ReportGeneratedEvent;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ReportSender {

  private static final Logger log = LoggerFactory.getLogger(ReportSender.class);
  private final ReportService reportService;
  private final CentralServerClientFactory clientFactory;
  private final ServerRepository serverRepository;
  private final SettingsService settingsService;
  private final CQLQueryService cqlQueryService;
  private final ObjectMapper objectMapper;

  public ReportSender(
      ReportService reportService,
      CentralServerClientFactory clientFactory,
      ServerRepository serverRepository,
      SettingsService settingsService,
      CQLQueryService cqlQueryService,
      ObjectMapper objectMapper) {
    this.reportService = reportService;
    this.clientFactory = clientFactory;
    this.serverRepository = serverRepository;
    this.settingsService = settingsService;
    this.cqlQueryService = cqlQueryService;
    this.objectMapper = objectMapper;
  }

  @EventListener
  @Transactional
  protected void onFinished(ReportGeneratedEvent event) {
    ReportDTO reportDTO = reportService.getById(event.getReportId());
    String agentId = settingsService.getSettings().getAgentId();
    serverRepository
        .findAllByStatusIs(ServerConnectionStatus.ACTIVE)
        .forEach(
            server -> {
              var client =
                  clientFactory.createClient(
                      agentId, server.getUrl(), server.getClientId(), server.getClientSecret());
              try {
                client.sendReport(reportDTO);
                String reportJson = objectMapper.writeValueAsString(reportDTO);
                server.addInteraction(
                    new ServerInteraction(InteractionType.COMMUNICATION, reportJson));
              } catch (Exception e) {
                log.error("Failed to send report to server {}", server.getUrl(), e);
              }
            });
  }
}
