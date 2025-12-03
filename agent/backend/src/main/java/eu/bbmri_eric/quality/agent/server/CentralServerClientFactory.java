package eu.bbmri_eric.quality.agent.server;

public interface CentralServerClientFactory {
  CentralServerClient createClient(
      String agentId, String serverUrl, String clientId, String clientSecret);
}
