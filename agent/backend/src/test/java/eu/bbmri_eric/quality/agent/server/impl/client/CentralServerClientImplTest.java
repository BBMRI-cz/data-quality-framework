package eu.bbmri_eric.quality.agent.server.impl.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.RegistrationCredentials;
import eu.bbmri_eric.quality.agent.server.RegistrationFailedException;
import eu.bbmri_eric.quality.agent.server.RegistrationFailureReason;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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
        .thenReturn(ResponseEntity.ok().build());

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
        .thenReturn(ResponseEntity.ok().build());

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
  void getManifestVersionQualityChecks_pythonQueryType_mapsToPython() {
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

    assertThat(checks.getFirst().getType()).isEqualTo(QualityCheckType.PYTHON);
  }

  @Test
  void getManifestVersionQualityChecks_unsupportedQueryType_leavesTypeNull() {
    stubLogin();
    QualityCheckListResponse.RemoteQualityCheckVersion version =
        new QualityCheckListResponse.RemoteQualityCheckVersion();
    version.setQuery("print('hello')");
    version.setType("RUBY");
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
        .thenReturn(ResponseEntity.ok().build());

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

  private void stubRegistration(ResponseEntity<AgentRegistrationResponse> response) {
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/agents"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(AgentRegistrationResponse.class)))
        .thenReturn(response);
  }

  private void stubRegistrationThrowing(Throwable failure) {
    when(restTemplate.exchange(
            eq(SERVER_URL + "/api/v1/agents"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(AgentRegistrationResponse.class)))
        .thenThrow(failure);
  }

  private static HttpServerErrorException serverError(HttpStatus status, String body) {
    return new HttpServerErrorException(
        status, status.name(), null, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
  }

  private static HttpClientErrorException clientError(HttpStatus status, String body) {
    return new HttpClientErrorException(
        status, status.name(), null, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
  }

  private static AgentRegistrationResponse completeRegistrationResponse() {
    AgentRegistrationResponse.UserInfo user = new AgentRegistrationResponse.UserInfo();
    user.setUsername("client-1");
    user.setTemporaryPassword("secret-1");
    AgentRegistrationResponse response = new AgentRegistrationResponse();
    response.setUser(user);
    return response;
  }

  @Test
  void register_success_returnsCredentials() {
    stubRegistration(new ResponseEntity<>(completeRegistrationResponse(), HttpStatus.OK));

    RegistrationCredentials credentials = client.register();

    assertThat(credentials.getClientId()).isEqualTo("client-1");
    assertThat(credentials.getClientSecret()).isEqualTo("secret-1");
  }

  @Test
  void register_conflictProblemDetail_throwsClassifiedException() {
    stubRegistrationThrowing(
        serverError(
            HttpStatus.CONFLICT,
            "{\"title\":\"Entity Already Exists\",\"status\":409,"
                + "\"detail\":\"Agent agent-1 already exists\"}"));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.CONFLICT);
              assertThat(ex.getHttpStatus()).hasValue(409);
              assertThat(ex).hasMessage("Agent agent-1 already exists");
            });
  }

  @Test
  void register_rateLimited_throwsRateLimitedException() {
    stubRegistrationThrowing(
        clientError(
            HttpStatus.TOO_MANY_REQUESTS,
            "{\"status\":429,\"detail\":\"Too many registration requests\"}"));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.RATE_LIMITED);
              assertThat(ex.getHttpStatus()).hasValue(429);
              assertThat(ex).hasMessage("Too many registration requests");
            });
  }

  @Test
  void register_serverErrorWithoutDetail_throwsServerErrorException() {
    stubRegistrationThrowing(
        serverError(HttpStatus.INTERNAL_SERVER_ERROR, "{\"title\":\"Internal Server Error\"}"));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.SERVER_ERROR);
              assertThat(ex.getHttpStatus()).hasValue(500);
              assertThat(ex).hasMessage("INTERNAL_SERVER_ERROR");
            });
  }

  @Test
  void register_unauthorized_throwsUnauthorizedException() {
    stubRegistrationThrowing(
        clientError(HttpStatus.UNAUTHORIZED, "{\"status\":401,\"detail\":\"Bad credentials\"}"));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.UNAUTHORIZED);
              assertThat(ex.getHttpStatus()).hasValue(401);
              assertThat(ex).hasMessage("Bad credentials");
            });
  }

  @Test
  void register_otherClientError_throwsClientErrorException() {
    stubRegistrationThrowing(clientError(HttpStatus.BAD_REQUEST, "{\"status\":400}"));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.CLIENT_ERROR);
              assertThat(ex.getHttpStatus()).hasValue(400);
              assertThat(ex).hasMessage("BAD_REQUEST");
            });
  }

  @Test
  void register_timeout_throwsTimeout() {
    stubRegistrationThrowing(
        new ResourceAccessException(
            "I/O error on POST request", new SocketTimeoutException("Read timed out")));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.TIMEOUT);
              assertThat(ex.getHttpStatus()).isEmpty();
              assertThat(ex).hasMessageContaining("Read timed out");
            });
  }

  @Test
  void register_connectTimeout_throwsTimeout() {
    stubRegistrationThrowing(
        new ResourceAccessException(
            "I/O error on POST request", new ConnectTimeoutException("Connect timed out")));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.TIMEOUT));
  }

  @Test
  void register_connectionRefused_throwsUnreachable() {
    stubRegistrationThrowing(
        new ResourceAccessException(
            "I/O error on POST request", new ConnectException("Connection refused")));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.UNREACHABLE);
              assertThat(ex).hasMessageContaining("Connection refused");
            });
  }

  @Test
  void register_unknownHost_throwsUnreachable() {
    stubRegistrationThrowing(
        new ResourceAccessException(
            "I/O error on POST request", new UnknownHostException("central.example.com")));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.UNREACHABLE));
  }

  @Test
  void register_tlsFailure_throwsTls() {
    stubRegistrationThrowing(
        new ResourceAccessException(
            "I/O error on POST request", new SSLHandshakeException("handshake failed")));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.TLS));
  }

  @Test
  void register_incompleteResponse_throwsIncompleteResponse() {
    AgentRegistrationResponse body = completeRegistrationResponse();
    body.getUser().setTemporaryPassword(null);
    stubRegistration(new ResponseEntity<>(body, HttpStatus.OK));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.INCOMPLETE_RESPONSE);
              assertThat(ex)
                  .hasMessage("Incomplete registration response: missing temporaryPassword");
            });
  }

  @Test
  void register_nonJsonErrorBody_usesStatusTextAsMessage() {
    stubRegistrationThrowing(
        serverError(HttpStatus.BAD_GATEWAY, "<html><body>Bad Gateway</body></html>"));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.SERVER_ERROR);
              assertThat(ex.getHttpStatus()).hasValue(502);
              assertThat(ex).hasMessage("BAD_GATEWAY");
            });
  }

  @Test
  void register_blankStatusTextWithoutDetail_usesHttpStatusCodeAsMessage() {
    stubRegistrationThrowing(
        new HttpClientErrorException(
            HttpStatus.BAD_GATEWAY, "", null, new byte[0], StandardCharsets.UTF_8));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.SERVER_ERROR);
              assertThat(ex.getHttpStatus()).hasValue(502);
              assertThat(ex).hasMessage("HTTP 502");
            });
  }

  @Test
  void register_otherRestClientFailure_throwsUnknown() {
    stubRegistrationThrowing(new RestClientException("Unexpected client failure"));

    assertThatThrownBy(() -> client.register())
        .isInstanceOfSatisfying(
            RegistrationFailedException.class,
            ex -> {
              assertThat(ex.getReason()).isEqualTo(RegistrationFailureReason.UNKNOWN);
              assertThat(ex).hasMessage("Unexpected client failure");
            });
  }
}
