package eu.bbmri_eric.quality.server.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Objects;

/**
 * Data Transfer Object for User information. Using Java Record as recommended for data-carrying
 * classes.
 */
@Schema(description = "User information data transfer object")
public class UserDTO {

  @Schema(description = "Username of the user", example = "admin")
  private String username;

  @Schema(description = "Unique identifier for the user", example = "user123")
  private String id;

  public UserDTO(
      @NotEmpty(message = "Username cannot be empty") String username,
      @NotEmpty(message = "ID cannot be empty") String id) {
    this.username = username;
    this.id = id;
  }

  UserDTO() {
    // Needed for modelmapper
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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
