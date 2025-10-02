package eu.bbmri_eric.quality.server.user;

import jakarta.validation.constraints.NotEmpty;

import java.util.Objects;

/**
 * Data Transfer Object for User information.
 * Using Java Record as recommended for data-carrying classes.
 */
public class UserDTO {

  private String username;

    public UserDTO(@NotEmpty(message = "Username cannot be empty") String username) {
        this.username = username;
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
