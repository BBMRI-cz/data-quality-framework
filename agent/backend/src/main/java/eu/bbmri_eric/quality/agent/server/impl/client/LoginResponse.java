package eu.bbmri_eric.quality.agent.server.impl.client;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
class LoginResponse {
  private String token;

  public LoginResponse() {}
}
