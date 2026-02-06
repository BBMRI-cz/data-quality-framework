package eu.bbmri_eric.quality.server.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Objects;
import java.util.Set;

/**
 * Data Transfer Object for User information. Using Java Record as recommended for data-carrying
 * classes.
 */
@Schema(description = "User information data transfer object")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

  @Schema(description = "Username of the user", example = "admin")
  private String username;

  @Schema(description = "Unique identifier for the user", example = "user123")
  private Long id;

  @Schema(description = "Temporary password", example = "123")
  private String temporaryPassword;

  @Schema(description = "Subject ID from the identity provider", example = "subject123")
  private String subjectId;

  @Schema(description = "Agent ID associated with the user", example = "agent123")
  private String agentId;

  private Set<UserRole> roles;

  public UserDTO(@NotEmpty(message = "Username cannot be empty") String username, Long id) {
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

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Set<UserRole> getRoles() {
    return roles;
  }

  public void setRoles(Set<UserRole> roles) {
    this.roles = roles;
  }

  public String getTemporaryPassword() {
    return temporaryPassword;
  }

  public void setTemporaryPassword(String temporaryPassword) {
    this.temporaryPassword = temporaryPassword;
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
    UserDTO userDTO = (UserDTO) o;
    return Objects.equals(username, userDTO.username)
        && Objects.equals(id, userDTO.id)
        && Objects.equals(temporaryPassword, userDTO.temporaryPassword)
        && Objects.equals(subjectId, userDTO.subjectId)
        && Objects.equals(agentId, userDTO.agentId)
        && Objects.equals(roles, userDTO.roles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, id, temporaryPassword, subjectId, agentId, roles);
  }

  public String getSubjectId() {
    return subjectId;
  }

  public void setSubjectId(String subjectId) {
    this.subjectId = subjectId;
  }
}
