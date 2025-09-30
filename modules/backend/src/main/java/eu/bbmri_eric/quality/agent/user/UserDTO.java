package eu.bbmri_eric.quality.agent.user;

import java.util.Objects;

public class UserDTO {
  private String username;
  private boolean defaultPassword;

  public UserDTO(String username) {
    this.username = username;
    this.defaultPassword = false;
  }

  public UserDTO(String username, boolean defaultPassword) {
    this.username = username;
    this.defaultPassword = defaultPassword;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public boolean isDefaultPassword() {
    return defaultPassword;
  }

  public void setDefaultPassword(boolean defaultPassword) {
    this.defaultPassword = defaultPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    UserDTO userDTO = (UserDTO) o;
    return Objects.equals(username, userDTO.username);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(username);
  }
}
