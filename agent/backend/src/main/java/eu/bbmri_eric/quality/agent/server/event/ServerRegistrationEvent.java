package eu.bbmri_eric.quality.agent.server.event;

import lombok.Getter;

/**
 * Event published when a server registration is initiated. Decouples server service from direct
 * communication with central servers.
 */
@Getter
public class ServerRegistrationEvent {
  private final String agentId;
  private final String serverUrl;

  public ServerRegistrationEvent(String agentId, String serverUrl) {
    this.agentId = agentId;
    this.serverUrl = serverUrl;
  }
}
