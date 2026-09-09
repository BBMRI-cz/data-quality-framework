package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.ManifestService;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ManifestServiceImplTest {

  @Autowired private ManifestService manifestService;

  @Autowired private ServerRepository serverRepository;

  @MockitoBean private CentralServerClientFactory clientFactory;

  private final CentralServerClient client = mock(CentralServerClient.class);
  private Server server;

  @BeforeEach
  void setUp() {
    server =
        serverRepository.save(
            new Server(
                "https://central.example.com",
                "Central",
                "client-id",
                "client-secret",
                ServerConnectionStatus.ACTIVE));
    when(clientFactory.createClient(
            anyString(), eq("https://central.example.com"), anyString(), anyString()))
        .thenReturn(client);
  }

  @Test
  void fetchManifests_returnsManifestsFromCentralServer() {
    ManifestDto manifest = new ManifestDto(1L, "Core checks", List.of());
    when(client.getManifests()).thenReturn(List.of(manifest));

    List<ManifestDto> manifests = manifestService.fetchManifests(server.getId());

    assertThat(manifests).hasSize(1);
    assertThat(manifests.getFirst().getRemoteId()).isEqualTo(1L);
  }

  @Test
  void fetchManifest_returnsManifestFromCentralServer() {
    ManifestDto manifest = new ManifestDto(5L, "Core checks", List.of());
    when(client.getManifest(5L)).thenReturn(manifest);

    ManifestDto result = manifestService.fetchManifest(server.getId(), 5L);

    assertThat(result.getRemoteId()).isEqualTo(5L);
    assertThat(result.getName()).isEqualTo("Core checks");
  }

  @Test
  void fetchManifests_unknownServer_throwsEntityNotFound() {
    assertThatThrownBy(() -> manifestService.fetchManifests("does-not-exist"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void fetchManifest_unknownServer_throwsEntityNotFound() {
    assertThatThrownBy(() -> manifestService.fetchManifest("does-not-exist", 1L))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void fetchManifests_remoteFailure_propagatesServerCommunicationException() {
    when(client.getManifests())
        .thenThrow(new ServerCommunicationException("Failed to fetch manifests"));

    assertThatThrownBy(() -> manifestService.fetchManifests(server.getId()))
        .isInstanceOf(ServerCommunicationException.class);
  }
}
