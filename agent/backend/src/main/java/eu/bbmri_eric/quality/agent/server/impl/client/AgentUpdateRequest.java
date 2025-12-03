package eu.bbmri_eric.quality.agent.server.impl.client;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
class AgentUpdateRequest {
  private String version;

  public AgentUpdateRequest(String version) {
    this.version = version;
  }
}
