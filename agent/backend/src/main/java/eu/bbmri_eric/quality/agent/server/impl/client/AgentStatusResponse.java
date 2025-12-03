package eu.bbmri_eric.quality.agent.server.impl.client;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
class AgentStatusResponse {
  private String id;
  private AgentStatus status;
  private String name;
  private String version;

  public AgentStatusResponse() {}
}
