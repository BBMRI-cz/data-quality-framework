package eu.bbmri_eric.quality.agent.server.impl.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/** Data Transfer Object for agent registration response from central servers. */
@Schema(description = "Agent registration response")
@JsonIgnoreProperties(ignoreUnknown = true)
class AgentRegistrationResponse {

  @Schema(description = "Agent information")
  private AgentInfo agent;

  @Schema(description = "User credentials information")
  private UserInfo user;

  /** Default constructor. */
  public AgentRegistrationResponse() {}

  /**
   * Gets the agent information.
   *
   * @return the agent info
   */
  public AgentInfo getAgent() {
    return agent;
  }

  /**
   * Sets the agent information.
   *
   * @param agent the agent info
   */
  public void setAgent(AgentInfo agent) {
    this.agent = agent;
  }

  /**
   * Gets the user credentials information.
   *
   * @return the user info
   */
  public UserInfo getUser() {
    return user;
  }

  /**
   * Sets the user credentials information.
   *
   * @param user the user info
   */
  public void setUser(UserInfo user) {
    this.user = user;
  }

  @Schema(description = "Agent information")
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AgentInfo {
    @Schema(description = "Agent ID")
    private String id;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  @Schema(description = "User credentials information")
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UserInfo {
    @Schema(description = "Username for authentication")
    private String username;

    @Schema(description = "Temporary password for authentication")
    private String temporaryPassword;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getTemporaryPassword() {
      return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
      this.temporaryPassword = temporaryPassword;
    }
  }
}
