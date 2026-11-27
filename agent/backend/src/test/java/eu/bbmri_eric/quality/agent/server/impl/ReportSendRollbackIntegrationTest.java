package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportCreateDTO;
import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * A report must only be sent to the central servers after the pipeline transaction committed. When
 * a step fails after {@code FinishingStep} published the generation event, the transaction is
 * rolled back and the report never exists locally, so nothing may ever have been sent: with the old
 * synchronous pre-commit listener this send happened mid-transaction and the server ended up with
 * values the agent never recorded.
 */
@SpringBootTest
@WithUserDetails("admin")
class ReportSendRollbackIntegrationTest {

  private static final String SERVER_URL = "https://rollback.example.com";

  @Autowired private ReportService reportService;
  @Autowired private ServerRepository serverRepository;
  @Autowired private SettingsService settingsService;

  @MockitoBean private CentralServerClientFactory clientFactory;
  @MockitoBean private DataStoreFactory dataStoreFactory;

  private final CentralServerClient client = mock(CentralServerClient.class);
  private final DataStore dataStore = mock(DataStore.class);

  private Server server;
  private Long reportId;

  @TestConfiguration
  static class FailingStepConfiguration {

    @Bean
    ReportPipelineStep failingStepAfterFinishing() {
      return new ReportPipelineStep() {

        @Override
        public Report execute(Report report) {
          throw new IllegalStateException("Simulated failure right after report generation");
        }

        @Override
        public int getOrder() {
          return FINISHING_ORDER + 1;
        }
      };
    }
  }

  @BeforeEach
  void setUp() {
    when(dataStoreFactory.resolveDataStore()).thenReturn(dataStore);
    when(dataStore.countPatients()).thenReturn(1000);
    when(dataStore.countSecondaryEntities()).thenReturn(2500);
    assertThat(settingsService.getSettings().getAgentId()).isNotBlank();
    when(clientFactory.createClient(any(), any(), any(), any())).thenReturn(client);
    server =
        serverRepository.save(
            new Server(
                SERVER_URL,
                "Rollback Test Central",
                "client-id",
                "client-secret",
                ServerConnectionStatus.ACTIVE));
  }

  @AfterEach
  void tearDown() {
    if (reportId != null) {
      reportService.delete(reportId);
    }
    serverRepository.deleteById(server.getId());
  }

  @Test
  void rollbackAfterFinishingStep_discardsReportAndSkipsSend() {
    assertThat(serverRepository.findAllByStatusIs(ServerConnectionStatus.ACTIVE))
        .anyMatch(existing -> existing.getId().equals(server.getId()));

    reportId = reportService.create(new ReportCreateDTO()).getId();

    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(
            () ->
                assertThat(reportService.findById(reportId).getStatus())
                    .isEqualTo(ReportStatus.FAILED));

    await()
        .during(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              verifyNoInteractions(clientFactory);
              verifyNoInteractions(client);
              assertThat(reportService.findById(reportId).getStatus())
                  .isEqualTo(ReportStatus.FAILED);
            });
  }
}
