package eu.bbmri_eric.quality.agent.server.impl.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class CentralServerClientImplTest {

  private static final String SERVER_URL = "https://central.example.com";

  private RestTemplate restTemplate;
  private CentralServerClientImpl client;

  @BeforeEach
  void setUp() {
    restTemplate = mock(RestTemplate.class);
    BuildProperties buildProperties = mock(BuildProperties.class);
    client =
        new CentralServerClientImpl(
            restTemplate, buildProperties, "agent-1", SERVER_URL, "client", "secret");
  }

  private void stubLogin() {
    LoginResponse login = new LoginResponse();
    login.setToken("token");
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/auth/login"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(LoginResponse.class)))
        .thenReturn(new ResponseEntity<>(login, HttpStatus.OK));
  }

  @Test
  void getManifests_returnsPublishedManifests() {
    stubLogin();
    ManifestListResponse.EmbeddedManifests embedded = new ManifestListResponse.EmbeddedManifests();
    ManifestDto manifest = new ManifestDto(1L, "Core checks", List.of());
    embedded.setManifests(List.of(manifest));
    ManifestListResponse response = new ManifestListResponse();
    response.setEmbedded(embedded);
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/manifests"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ManifestListResponse.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    List<ManifestDto> manifests = client.getManifests();

    assertThat(manifests).hasSize(1);
    assertThat(manifests.getFirst().getRemoteId()).isEqualTo(1L);
    assertThat(manifests.getFirst().getName()).isEqualTo("Core checks");
  }

  @Test
  void getManifests_emptyResponse_returnsEmptyList() {
    stubLogin();
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/manifests"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ManifestListResponse.class)))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

    assertThat(client.getManifests()).isEmpty();
  }

  @Test
  void getManifest_returnsManifestWithVersions() {
    stubLogin();
    ManifestDto manifest = new ManifestDto(7L, "Core checks", List.of());
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/manifests/7"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ManifestDto.class)))
        .thenReturn(new ResponseEntity<>(manifest, HttpStatus.OK));

    ManifestDto result = client.getManifest(7L);

    assertThat(result.getRemoteId()).isEqualTo(7L);
    assertThat(result.getName()).isEqualTo("Core checks");
  }

  @Test
  void getManifests_serverUnreachable_throwsServerCommunicationException() {
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/auth/login"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(LoginResponse.class)))
        .thenThrow(new RestClientException("Connection refused"));

    assertThatThrownBy(() -> client.getManifests())
        .isInstanceOf(ServerCommunicationException.class)
        .hasMessageContaining("fetch manifests")
        .hasMessageContaining(SERVER_URL);
  }

  @Test
  void getManifest_emptyBody_throwsServerCommunicationException() {
    stubLogin();
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/manifests/7"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ManifestDto.class)))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

    assertThatThrownBy(() -> client.getManifest(7L))
        .isInstanceOf(ServerCommunicationException.class);
  }

  @Test
  void getManifestVersionQualityChecks_mapsRemoteChecksToQualityCheckDtos() {
    stubLogin();
    QualityCheckListResponse.RemoteQualityCheckVersion version =
        new QualityCheckListResponse.RemoteQualityCheckVersion();
    version.setId(5L);
    version.setVersion(3);
    version.setQuery("SELECT COUNT(*) FROM patients");
    version.setHash("abc123");
    version.setType("SQL");
    QualityCheckListResponse.RemoteCategory category =
        new QualityCheckListResponse.RemoteCategory();
    category.setId(2L);
    category.setName("Completeness");
    category.setColorHex("#FF5733");
    QualityCheckListResponse.RemoteQualityCheck check =
        new QualityCheckListResponse.RemoteQualityCheck();
    check.setId(1L);
    check.setName("Patient Count");
    check.setDescription("Counts patients");
    check.setWarningThreshold(10.0);
    check.setErrorThreshold(30.4);
    check.setCategory(category);
    check.setVersions(List.of(version));
    QualityCheckListResponse.EmbeddedQualityChecks embedded =
        new QualityCheckListResponse.EmbeddedQualityChecks();
    embedded.setQualityChecks(List.of(check));
    QualityCheckListResponse response = new QualityCheckListResponse();
    response.setEmbedded(embedded);
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/manifests/7/versions/42/quality-checks"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(QualityCheckListResponse.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    List<QualityCheckDTO> checks = client.getManifestVersionQualityChecks(7L, 42L);

    assertThat(checks).hasSize(1);
    QualityCheckDTO dto = checks.getFirst();
    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getName()).isEqualTo("Patient Count");
    assertThat(dto.getDescription()).isEqualTo("Counts patients");
    assertThat(dto.getQuery()).isEqualTo("SELECT COUNT(*) FROM patients");
    assertThat(dto.getType()).isEqualTo(QualityCheckType.SQL);
    assertThat(dto.getWarningThreshold()).isEqualTo(10);
    assertThat(dto.getErrorThreshold()).isEqualTo(30);
    assertThat(dto.getCategory().getName()).isEqualTo("Completeness");
  }

  @Test
  void getManifestVersionQualityChecks_unsupportedQueryType_leavesTypeNull() {
    stubLogin();
    QualityCheckListResponse.RemoteQualityCheckVersion version =
        new QualityCheckListResponse.RemoteQualityCheckVersion();
    version.setQuery("print('hello')");
    version.setType("PYTHON");
    QualityCheckListResponse.RemoteQualityCheck check =
        new QualityCheckListResponse.RemoteQualityCheck();
    check.setId(1L);
    check.setVersions(List.of(version));
    QualityCheckListResponse.EmbeddedQualityChecks embedded =
        new QualityCheckListResponse.EmbeddedQualityChecks();
    embedded.setQualityChecks(List.of(check));
    QualityCheckListResponse response = new QualityCheckListResponse();
    response.setEmbedded(embedded);
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/manifests/7/versions/42/quality-checks"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(QualityCheckListResponse.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    List<QualityCheckDTO> checks = client.getManifestVersionQualityChecks(7L, 42L);

    assertThat(checks.getFirst().getType()).isNull();
  }

  @Test
  void getManifestVersionQualityChecks_emptyResponse_returnsEmptyList() {
    stubLogin();
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/manifests/7/versions/42/quality-checks"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(QualityCheckListResponse.class)))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

    assertThat(client.getManifestVersionQualityChecks(7L, 42L)).isEmpty();
  }

  @Test
  void getManifestVersionQualityChecks_serverUnreachable_throwsServerCommunicationException() {
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/auth/login"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(LoginResponse.class)))
        .thenThrow(new RestClientException("Connection refused"));

    assertThatThrownBy(() -> client.getManifestVersionQualityChecks(7L, 42L))
        .isInstanceOf(ServerCommunicationException.class)
        .hasMessageContaining("fetch quality checks")
        .hasMessageContaining(SERVER_URL);
  }
}
