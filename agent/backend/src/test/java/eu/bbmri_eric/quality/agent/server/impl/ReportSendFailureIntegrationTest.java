package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportCreateDTO;
import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.domain.InteractionType;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * A send failure after the report has committed must leave the report GENERATED and must not flip
 * it to FAILED: the pipeline transaction is already durable, and the next run redrawing fresh noise
 * would double-spend the privacy budget for data the server never received. Because unchanged raw
 * counts make the release rule reuse the previously published noisy value, the next run resends
 * exactly the same values without any resend ledger.
 */
@SpringBootTest
@WithUserDetails("admin")
class ReportSendFailureIntegrationTest {

  private static final String SERVER_URL = "https://send-failure.example.com";

  @Autowired private ReportService reportService;
  @MockitoSpyBean private ServerRepository serverRepository;
  @Autowired private SettingsService settingsService;

  @MockitoBean private CentralServerClientFactory clientFactory;
  @MockitoBean private DataStoreFactory dataStoreFactory;

  private final CentralServerClient client = mock(CentralServerClient.class);
  private final DataStore dataStore = mock(DataStore.class);
  private final ArgumentCaptor<ObfuscatedReportDTO> sentReport =
      ArgumentCaptor.forClass(ObfuscatedReportDTO.class);

  private Server server;
  private final List<Long> createdReportIds = new ArrayList<>();

  @BeforeEach
  void setUp() {
    when(dataStoreFactory.resolveDataStore()).thenReturn(dataStore);
    when(dataStore.countPatients()).thenReturn(1000);
    when(dataStore.countSecondaryEntities()).thenReturn(2500);
    assertThat(settingsService.getSettings().getAgentId()).isNotBlank();
    server =
        serverRepository.save(
            new Server(
                SERVER_URL,
                "Send Failure Test Central",
                "client-id",
                "client-secret",
                ServerConnectionStatus.ACTIVE));
  }

  @AfterEach
  void tearDown() {
    createdReportIds.forEach(reportService::delete);
    serverRepository.deleteById(server.getId());
  }

  @Test
  void sendFailureAfterCommit_keepsReportGeneratedAndReusesValues() throws Exception {
    when(clientFactory.createClient(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Simulated client creation failure"))
        .thenReturn(client);

    Long firstReportId = createReport();
    awaitReportStatus(firstReportId, ReportStatus.GENERATED);

    // Wait until the async sender has consumed the first stubbed createClient call, so the
    // thenThrow(...).thenReturn(...) sequence cannot be consumed out of order by the second run.
    verify(clientFactory, timeout(10_000)).createClient(any(), any(), any(), any());
    verify(client, never()).sendReport(any());

    Integer firstPublishedPatients =
        reportService.getObfuscatedById(firstReportId).getTotalPatients();
    assertThat(firstPublishedPatients).isNotNull();
    assertThat(reportService.findById(firstReportId).getStatus()).isEqualTo(ReportStatus.GENERATED);

    Long secondReportId = createReport();
    awaitReportStatus(secondReportId, ReportStatus.GENERATED);

    verify(client, timeout(10_000)).sendReport(sentReport.capture());

    assertThat(reportService.findById(secondReportId).getStatus())
        .isEqualTo(ReportStatus.GENERATED);
    assertThat(reportService.findById(firstReportId).getStatus()).isEqualTo(ReportStatus.GENERATED);
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(
                        serverRepository
                            .findWithInteractionsByUrl(SERVER_URL)
                            .orElseThrow()
                            .getInteractions())
                    .anyMatch(
                        interaction -> interaction.getType() == InteractionType.COMMUNICATION));
    assertThat(sentReport.getValue().getTotalPatients()).isEqualTo(firstPublishedPatients);
  }

  @Test
  void successfulSend_concurrentServerUpdate_preservesChangesAndInteractions() throws Exception {
    String updatedClientSecret = "updated-client-secret";
    String concurrentUpdateDescription = "Concurrent administrator update";
    when(clientFactory.createClient(any(), any(), any(), any())).thenReturn(client);
    doAnswer(
            invocation -> {
              Server concurrentlyUpdatedServer =
                  serverRepository.findWithInteractionsByUrl(SERVER_URL).orElseThrow();
              concurrentlyUpdatedServer.setClientSecret(updatedClientSecret);
              concurrentlyUpdatedServer.addInteraction(
                  new ServerInteraction(InteractionType.UPDATE, concurrentUpdateDescription));
              serverRepository.save(concurrentlyUpdatedServer);
              // Ignore setup and this deliberate update; the sender must not save a Server itself.
              clearInvocations(serverRepository);
              return null;
            })
        .when(client)
        .sendReport(any());

    Long reportId = createReport();
    awaitReportStatus(reportId, ReportStatus.GENERATED);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              Server freshlyQueriedServer =
                  serverRepository.findWithInteractionsByUrl(SERVER_URL).orElseThrow();
              assertThat(freshlyQueriedServer.getClientSecret()).isEqualTo(updatedClientSecret);
              assertThat(freshlyQueriedServer.getInteractions())
                  .anyMatch(
                      interaction ->
                          interaction.getType() == InteractionType.UPDATE
                              && interaction.getDescription().equals(concurrentUpdateDescription))
                  .anyMatch(interaction -> interaction.getType() == InteractionType.COMMUNICATION);
            });
    verify(serverRepository, never())
        .save(argThat(savedServer -> savedServer.getId().equals(server.getId())));
  }

  private Long createReport() {
    Long id = reportService.create(new ReportCreateDTO()).getId();
    createdReportIds.add(id);
    return id;
  }

  private void awaitReportStatus(Long reportId, ReportStatus expected) {
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(
            () -> assertThat(reportService.findById(reportId).getStatus()).isEqualTo(expected));
  }
}
