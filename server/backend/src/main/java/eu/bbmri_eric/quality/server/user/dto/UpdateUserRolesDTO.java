package eu.bbmri_eric.quality.server.user.dto;

import eu.bbmri_eric.quality.server.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

/** DTO for updating the roles assigned to a user. */
@Schema(name = "Update User Roles", description = "Data for updating a user's roles")
public class UpdateUserRolesDTO {

  @NotEmpty(message = "Roles cannot be empty")
  @Schema(
      description = "Roles to assign to the user, replacing any existing roles",
      example = "[\"HUMAN_USER\"]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Set<UserRole> roles;

  public UpdateUserRolesDTO() {}

  public UpdateUserRolesDTO(Set<UserRole> roles) {
    this.roles = roles;
  }

  public Set<UserRole> getRoles() {
    return roles;
  }

  public void setRoles(Set<UserRole> roles) {
    this.roles = roles;
  }
}
