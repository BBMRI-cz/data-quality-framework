package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import eu.bbmri_eric.quality.agent.server.dto.ManifestVersionDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManifestIntegrationTests {

  private static final String API_SERVERS_MANIFESTS = "/api/servers/%s/manifests";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

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
  @WithUserDetails("admin")
  void listManifests_returnsManifestsFromCentralServer() throws Exception {
    ManifestDto manifest = new ManifestDto(1L, "Core checks", List.of());
    when(client.getManifests()).thenReturn(List.of(manifest));

    mockMvc
        .perform(get(API_SERVERS_MANIFESTS.formatted(server.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].remoteId").value(1))
        .andExpect(jsonPath("$[0].name").value("Core checks"))
        .andExpect(jsonPath("$[0].versions").isArray());
  }

  @Test
  @WithUserDetails("admin")
  void findManifest_returnsVersionsWithSignedBody() throws Exception {
    JsonNode body =
        objectMapper.readTree(
            "{\"manifest_id\":1,\"generated_at\":\"2026-08-13T10:00:00Z\",\"checks\":[]}");
    ManifestVersionDto version =
        new ManifestVersionDto(
            42L, 1, Instant.parse("2026-08-13T10:00:00Z"), body, "MEUCIBd", "central-signing");
    when(client.getManifest(1L)).thenReturn(new ManifestDto(1L, "Core checks", List.of(version)));

    mockMvc
        .perform(get(API_SERVERS_MANIFESTS.formatted(server.getId()) + "/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remoteId").value(1))
        .andExpect(jsonPath("$.versions[0].remoteId").value(42))
        .andExpect(jsonPath("$.versions[0].version").value(1))
        .andExpect(jsonPath("$.versions[0].generatedAt").value("2026-08-13T10:00:00Z"))
        .andExpect(jsonPath("$.versions[0].body.manifest_id").value(1))
        .andExpect(jsonPath("$.versions[0].signature").value("MEUCIBd"))
        .andExpect(jsonPath("$.versions[0].keyId").value("central-signing"));
  }

  @Test
  @WithUserDetails("admin")
  void listManifests_unknownServer_returnsNotFound() throws Exception {
    mockMvc
        .perform(get(API_SERVERS_MANIFESTS.formatted("does-not-exist")))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void listManifests_serverUnreachable_returnsBadGateway() throws Exception {
    when(client.getManifests())
        .thenThrow(new ServerCommunicationException("Failed to fetch manifests"));

    mockMvc
        .perform(get(API_SERVERS_MANIFESTS.formatted(server.getId())))
        .andExpect(status().isBadGateway());
  }

  @Test
  void listManifests_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(get(API_SERVERS_MANIFESTS.formatted(server.getId())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("admin")
  void findVersionQualityChecks_returnsQualityChecksFromCentralServer() throws Exception {
    QualityCheckDTO check = new QualityCheckDTO();
    check.setId(1L);
    check.setName("Patient Count");
    check.setDescription("Counts patients");
    check.setQuery("SELECT COUNT(*) FROM patients");
    check.setType(QualityCheckType.SQL);
    check.setWarningThreshold(10);
    check.setErrorThreshold(30);
    when(client.getManifestVersionQualityChecks(1L, 2L)).thenReturn(List.of(check));

    mockMvc
        .perform(
            get(API_SERVERS_MANIFESTS.formatted(server.getId()) + "/1/versions/2/quality-checks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Patient Count"))
        .andExpect(jsonPath("$[0].query").value("SELECT COUNT(*) FROM patients"))
        .andExpect(jsonPath("$[0].type").value("SQL"))
        .andExpect(jsonPath("$[0].warningThreshold").value(10))
        .andExpect(jsonPath("$[0].errorThreshold").value(30));
  }

  @Test
  @WithUserDetails("admin")
  void findVersionQualityChecks_unknownServer_returnsNotFound() throws Exception {
    mockMvc
        .perform(
            get(API_SERVERS_MANIFESTS.formatted("does-not-exist") + "/1/versions/2/quality-checks"))
        .andExpect(status().isNotFound());
  }

  @Test
  void findVersionQualityChecks_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            get(API_SERVERS_MANIFESTS.formatted(server.getId()) + "/1/versions/2/quality-checks"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("admin")
  void downloadManifest_persistsManifestAndQualityChecks() throws Exception {
    stubSignedServerAndManifest();
    QualityCheckDTO check = new QualityCheckDTO();
    check.setId(7L);
    check.setName("Patient Count");
    check.setDescription("Counts patients");
    check.setQuery("SELECT COUNT(*) FROM patients");
    check.setType(QualityCheckType.SQL);
    check.setWarningThreshold(10);
    check.setErrorThreshold(30);
    when(client.getManifestVersionQualityChecks(1L, 42L)).thenReturn(List.of(check));
    when(qualityCheckService.create(any(QualityCheckCreateDTO.class)))
        .thenAnswer(
            invocation -> {
              QualityCheckCreateDTO createDTO = invocation.getArgument(0);
              QualityCheckDTO created = new QualityCheckDTO();
              created.setId(100L);
              created.setName(createDTO.getName());
              return created;
            });

    mockMvc
        .perform(post(API_SERVERS_MANIFESTS.formatted(server.getId()) + "/1/versions/2/download"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remoteId").value(1))
        .andExpect(jsonPath("$.name").value("Core checks"))
        .andExpect(jsonPath("$.installedVersion").value(2))
        .andExpect(jsonPath("$.installedChecks").value(1));

    assertThat(manifestRepository.findByServerIdAndRemoteId(server.getId(), 1L)).isPresent();
  }

  @Test
  @WithUserDetails("admin")
  void downloadManifest_invalidSignature_returnsBadGateway() throws Exception {
    stubSignedServerAndManifest();
    ManifestVersionDto version = TestManifestSigner.signedVersion(42L, 2);
    version.setSignature(TestManifestSigner.sign("{\"manifest_id\":999}"));
    when(client.getManifest(1L)).thenReturn(new ManifestDto(1L, "Core checks", List.of(version)));

    mockMvc
        .perform(post(API_SERVERS_MANIFESTS.formatted(server.getId()) + "/1/versions/2/download"))
        .andExpect(status().isBadGateway());

    assertThat(manifestRepository.count()).isZero();
  }

  @Test
  @WithUserDetails("admin")
  void downloadManifest_unsupportedCheckType_returnsBadGateway() throws Exception {
    stubSignedServerAndManifest();
    QualityCheckDTO untyped = new QualityCheckDTO();
    untyped.setName("Legacy Check");
    when(client.getManifestVersionQualityChecks(1L, 42L)).thenReturn(List.of(untyped));

    mockMvc
        .perform(post(API_SERVERS_MANIFESTS.formatted(server.getId()) + "/1/versions/2/download"))
        .andExpect(status().isBadGateway());

    assertThat(manifestRepository.count()).isZero();
  }

  private void stubSignedServerAndManifest() {
    server.setPublicKey(TestManifestSigner.publicKeyPem());
    server = serverRepository.save(server);
    when(client.getManifest(1L))
        .thenReturn(
            new ManifestDto(1L, "Core checks", List.of(TestManifestSigner.signedVersion(42L, 2))));
  }

  @Test
  @WithUserDetails("admin")
  void downloadManifest_unknownServer_returnsNotFound() throws Exception {
    mockMvc
        .perform(post(API_SERVERS_MANIFESTS.formatted("does-not-exist") + "/1/versions/2/download"))
        .andExpect(status().isNotFound());
  }

  @Test
  void downloadManifest_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(post(API_SERVERS_MANIFESTS.formatted(server.getId()) + "/1/versions/2/download"))
        .andExpect(status().isUnauthorized());
  }
}
