package eu.bbmri_eric.quality.agent.server.impl.client;

import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Factory for creating CentralServerClient instances with specific server configurations.
 *
 * <p>Encapsulates the creation of CentralServerClient instances, allowing clients to request a
 * client configured for a specific server without managing dependencies directly.
 */
@Component
class CentralServerClientFactoryImpl implements CentralServerClientFactory {

  private final RestTemplate restTemplate;
  private final BuildProperties buildProperties;

  public CentralServerClientFactoryImpl(
      RestTemplate restTemplate, BuildProperties buildProperties) {
    this.restTemplate = restTemplate;
    this.buildProperties = buildProperties;
  }

  /**
   * Creates a CentralServerClient configured for the given server.
   *
   * @param agentId the agent identifier
   * @param serverUrl the base URL of the central server
   * @param clientId the client ID for authentication
   * @param clientSecret the client secret for authentication
   * @return a configured CentralServerClient instance
   */
  @Override
  public CentralServerClient createClient(
      String agentId, String serverUrl, String clientId, String clientSecret) {
    return new CentralServerClientImpl(
        restTemplate, buildProperties, agentId, serverUrl, clientId, clientSecret);
  }
}
