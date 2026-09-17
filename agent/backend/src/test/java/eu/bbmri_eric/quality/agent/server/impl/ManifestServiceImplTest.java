package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.ManifestService;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.domain.Manifest;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDownloadDto;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import eu.bbmri_eric.quality.agent.server.dto.ManifestVersionDto;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
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

  @Autowired private ManifestRepository manifestRepository;

  @MockitoBean private CentralServerClientFactory clientFactory;

  @MockitoBean private QualityCheckService qualityCheckService;

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

  @Test
  void fetchVersionQualityChecks_returnsQualityChecksFromCentralServer() {
    QualityCheckDTO check = new QualityCheckDTO();
    check.setId(1L);
    check.setName("Patient Count");
    when(client.getManifestVersionQualityChecks(1L, 2L)).thenReturn(List.of(check));

    List<QualityCheckDTO> checks =
        manifestService.fetchVersionQualityChecks(server.getId(), 1L, 2L);

    assertThat(checks).hasSize(1);
    assertThat(checks.getFirst().getName()).isEqualTo("Patient Count");
  }

  @Test
  void fetchVersionQualityChecks_unknownServer_throwsEntityNotFound() {
    assertThatThrownBy(() -> manifestService.fetchVersionQualityChecks("does-not-exist", 1L, 2L))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void downloadManifest_persistsManifestAndQualityChecks() {
    stubRemoteManifestWithVersion();
    QualityCheckDTO remoteCheck = remoteCheck("Patient Count");
    when(client.getManifestVersionQualityChecks(1L, 42L)).thenReturn(List.of(remoteCheck));
    AtomicLong nextId = new AtomicLong(100L);
    when(qualityCheckService.create(any(QualityCheckCreateDTO.class)))
        .thenAnswer(
            invocation -> {
              QualityCheckCreateDTO createDTO = invocation.getArgument(0);
              QualityCheckDTO created = new QualityCheckDTO();
              created.setId(nextId.getAndIncrement());
              created.setName(createDTO.getName());
              return created;
            });

    ManifestDownloadDto result = manifestService.downloadManifest(server.getId(), 1L, 2);

    assertThat(result.getRemoteId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo("Core checks");
    assertThat(result.getInstalledVersion()).isEqualTo(2);
    assertThat(result.getInstalledChecks()).isEqualTo(1);
    Manifest persisted =
        manifestRepository.findByServerIdAndRemoteId(server.getId(), 1L).orElseThrow();
    assertThat(persisted.getInstalledVersion()).isEqualTo(2);
    assertThat(persisted.getQualityCheckIds()).containsExactly(100L);
  }

  @Test
  void downloadManifest_secondDownload_updatesExistingManifest() {
    stubRemoteManifestWithVersion();
    when(client.getManifestVersionQualityChecks(1L, 42L)).thenReturn(List.of());
    Manifest existing = new Manifest(1L, "Old name", server);
    existing.setInstalledVersion(1);
    manifestRepository.save(existing);

    ManifestDownloadDto result = manifestService.downloadManifest(server.getId(), 1L, 2);

    assertThat(result.getId()).isEqualTo(existing.getId());
    assertThat(result.getName()).isEqualTo("Core checks");
    assertThat(result.getInstalledVersion()).isEqualTo(2);
    assertThat(manifestRepository.findByServerIdAndRemoteId(server.getId(), 1L)).isPresent();
    assertThat(manifestRepository.count()).isOne();
  }

  @Test
  void downloadManifest_unknownVersion_throwsEntityNotFound() {
    stubRemoteManifestWithVersion();

    assertThatThrownBy(() -> manifestService.downloadManifest(server.getId(), 1L, 99))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void downloadManifest_unknownServer_throwsEntityNotFound() {
    assertThatThrownBy(() -> manifestService.downloadManifest("does-not-exist", 1L, 2))
        .isInstanceOf(EntityNotFoundException.class);
  }

  private void stubRemoteManifestWithVersion() {
    ManifestVersionDto version =
        new ManifestVersionDto(42L, 2, Instant.parse("2026-08-13T10:00:00Z"), null, "sig", "key");
    when(client.getManifest(1L)).thenReturn(new ManifestDto(1L, "Core checks", List.of(version)));
  }

  private QualityCheckDTO remoteCheck(String name) {
    QualityCheckDTO check = new QualityCheckDTO();
    check.setId(7L);
    check.setName(name);
    check.setDescription("Description of " + name);
    check.setQuery("SELECT COUNT(*) FROM patients");
    check.setWarningThreshold(10);
    check.setErrorThreshold(30);
    return check;
  }
}
