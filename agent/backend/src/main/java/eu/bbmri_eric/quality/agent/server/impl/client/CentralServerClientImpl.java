package eu.bbmri_eric.quality.agent.server.impl.client;

import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.RegistrationCredentials;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of CentralServerClient for handling communication with central servers. Manages
 * agent registration, status checks, and health verification with remote servers.
 */
class CentralServerClientImpl implements CentralServerClient {

  private static final Logger log = LoggerFactory.getLogger(CentralServerClientImpl.class);

  // API Endpoints
  private static final String AGENTS_ENDPOINT = "/api/v1/agents";
  private static final String LOGIN_ENDPOINT = "/api/auth/login";

  private final RestTemplate restTemplate;
  private final BuildProperties buildProperties;
  private final String agentId;
  private final String serverUrl;
  private final String clientId;
  private final String clientSecret;

  public CentralServerClientImpl(
      RestTemplate restTemplate,
      BuildProperties buildProperties,
      String agentId,
      String serverUrl,
      String clientId,
      String clientSecret) {
    this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate cannot be null");
    this.buildProperties = buildProperties;
    this.agentId = Objects.requireNonNull(agentId, "Agent ID cannot be null or empty");
    this.serverUrl = Objects.requireNonNull(serverUrl, "Server URL cannot be null or empty");
    this.clientId = Objects.requireNonNull(clientId, "Client ID cannot be null or empty");
    this.clientSecret =
        Objects.requireNonNull(clientSecret, "Client Secret cannot be null or empty");
  }

  /**
   * Registers an agent with the central server.
   *
   * @return registration credentials containing clientId and clientSecret, or null if registration
   *     fails
   */
  @Override
  public RegistrationCredentials register() {
    AgentRegistrationRequest request =
        new AgentRegistrationRequest(agentId, buildProperties.getVersion());
    HttpEntity<AgentRegistrationRequest> requestEntity = createJsonHttpEntity(request);
    String registrationUrl = buildApiUrl(AGENTS_ENDPOINT);
    ResponseEntity<AgentRegistrationResponse> response =
        restTemplate.exchange(
            registrationUrl, HttpMethod.POST, requestEntity, AgentRegistrationResponse.class);
    return parseRegistrationResponse(response);
  }

  /**
   * Checks the registration status of an agent with the central server.
   *
   * @return the current connection status
   */
  @Override
  public ServerConnectionStatus checkRegistrationStatus() {
    String token = authenticateWithServer();
    return queryAgentStatus(token);
  }

  /** Performs a health check on the central server. */
  @Override
  public void healthCheck() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Updates the agent version on the central server.
   *
   * @param version the version string to update
   */
  @Override
  public void updateAgentVersion(String version) {
    String token = authenticateWithServer();
    String updateUrl = buildApiUrl(AGENTS_ENDPOINT + "/" + agentId);
    AgentUpdateRequest updateRequest = new AgentUpdateRequest(version);
    HttpHeaders headers = createDefaultHeaders();
    headers.setBearerAuth(token);
    HttpEntity<AgentUpdateRequest> requestEntity = new HttpEntity<>(updateRequest, headers);
    restTemplate.exchange(updateUrl, HttpMethod.PATCH, requestEntity, Void.class);
    log.info("Successfully updated agent version to {} on server {}", version, serverUrl);
  }

  @Override
  public void sendReport(ReportDTO reportDTO) {
    String token = authenticateWithServer();
    String reportUrl = buildApiUrl(AGENTS_ENDPOINT + "/" + agentId + "/reports");
    HttpHeaders headers = createDefaultHeaders();
    headers.setBearerAuth(token);
    HttpEntity<ReportDTO> requestEntity = new HttpEntity<>(reportDTO, headers);
    restTemplate.exchange(reportUrl, HttpMethod.POST, requestEntity, Void.class);
    log.info("Successfully sent report to server {}", serverUrl);
  }

  /**
   * Authenticates with the central server and retrieves a bearer token.
   *
   * @return the authentication token, or null if authentication fails
   */
  private String authenticateWithServer() {
    String loginUrl = buildApiUrl(LOGIN_ENDPOINT);
    LoginRequest loginRequest = new LoginRequest(clientId, clientSecret);
    HttpEntity<LoginRequest> requestEntity = createJsonHttpEntity(loginRequest);

    ResponseEntity<LoginResponse> response =
        restTemplate.exchange(loginUrl, HttpMethod.POST, requestEntity, LoginResponse.class);

    LoginResponse loginBody = response.getBody();
    if (loginBody == null || loginBody.getToken() == null) {
      log.error("Authentication failed with server {} - no token received", serverUrl);
      throw new RestClientException("Authentication failed with server " + serverUrl);
    }
    return loginBody.getToken();
  }

  /**
   * Queries the status of an agent from the central server.
   *
   * @param token the authentication token
   * @return the agent's connection status
   */
  private ServerConnectionStatus queryAgentStatus(String token) {
    String checkUrl = buildApiUrl(AGENTS_ENDPOINT + "/" + agentId);
    HttpHeaders headers = createDefaultHeaders();
    headers.setBearerAuth(token);
    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
    ResponseEntity<AgentStatusResponse> response =
        restTemplate.exchange(checkUrl, HttpMethod.GET, requestEntity, AgentStatusResponse.class);
    return mapAgentStatusToConnectionStatus(response.getBody());
  }

  /**
   * Maps an agent status to a server connection status.
   *
   * @param agentStatus the agent status response
   * @return the corresponding connection status
   */
  private ServerConnectionStatus mapAgentStatusToConnectionStatus(AgentStatusResponse agentStatus) {
    if (agentStatus == null || agentStatus.getStatus() == null) {
      return ServerConnectionStatus.PENDING;
    }
    return switch (agentStatus.getStatus()) {
      case ACTIVE -> ServerConnectionStatus.ACTIVE;
      case INACTIVE -> ServerConnectionStatus.INACTIVE;
      default -> ServerConnectionStatus.PENDING;
    };
  }

  /**
   * Handles the response from an agent registration request.
   *
   * @param response the registration response
   * @return registration credentials, or null if response is incomplete
   */
  private RegistrationCredentials parseRegistrationResponse(
      ResponseEntity<AgentRegistrationResponse> response) {
    AgentRegistrationResponse registrationResponse = response.getBody();
    if (registrationResponse == null
        || registrationResponse.getUser() == null
        || registrationResponse.getUser().getUsername() == null
        || registrationResponse.getUser().getTemporaryPassword() == null) {
      throw new IllegalStateException(
          "Incomplete registration response for agent " + agentId + " at server " + serverUrl);
    }
    String clientId = registrationResponse.getUser().getUsername();
    String clientSecret = registrationResponse.getUser().getTemporaryPassword();
    return new RegistrationCredentials(clientId, clientSecret);
  }

  /**
   * Creates an HTTP entity with JSON content type for the given payload.
   *
   * @param payload the request payload
   * @param <T> the type of the payload
   * @return an HTTP entity with appropriate headers
   */
  private <T> HttpEntity<T> createJsonHttpEntity(T payload) {
    HttpHeaders headers = createDefaultHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(payload, headers);
  }

  /**
   * Creates default HTTP headers.
   *
   * @return HTTP headers with Accept: application/json
   */
  private HttpHeaders createDefaultHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /**
   * Builds a complete API URL from server base URL and endpoint.
   *
   * @param endpoint the API endpoint path
   * @return the complete API URL
   */
  private String buildApiUrl(String endpoint) {
    return serverUrl + endpoint;
  }
}
