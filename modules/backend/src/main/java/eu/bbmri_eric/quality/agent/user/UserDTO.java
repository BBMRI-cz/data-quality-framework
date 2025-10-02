package eu.bbmri_eric.quality.agent.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "User data transfer object containing user information")
public class UserDTO {

  @Schema(description = "Username of the user", example = "admin")
  private String username;

  @Schema(description = "Unique identifier of the user", example = "1")
  private Long userId;

  @Schema(description = "Flag indicating if the user is using default password", example = "true")
  private boolean defaultPassword;

  public UserDTO(String username, Long userId) {
    this.userId = userId;
    this.username = username;
    this.defaultPassword = false;
  }

  public UserDTO(String username, boolean defaultPassword, Long userId) {
    this.userId = userId;
    this.username = username;
    this.defaultPassword = defaultPassword;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
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
