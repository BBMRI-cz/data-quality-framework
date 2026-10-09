package eu.bbmri_eric.quality.server.user.dto;

import java.util.Objects;

public class UserCreateDTO {
  private String username;
  private String agentId;

  public UserCreateDTO(String username, String agentId) {
    this.username = username;
    this.agentId = agentId;
  }

  public UserCreateDTO() {}

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    UserCreateDTO that = (UserCreateDTO) o;
    return Objects.equals(username, that.username) && Objects.equals(agentId, that.agentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, agentId);
  }
}
