package eu.bbmri_eric.quality.agent.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Request object for changing user password")
@Getter
@Setter
@AllArgsConstructor
public class PasswordChangeRequest {

  @Schema(
      description = "Current password of the user",
      example = "oldPassword123",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String currentPassword;

  @Pattern(
      regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>_-]{8,}$",
      message =
          "Password must be at least 8 characters long and contain only letters, digits or special characters")
  @Schema(
      description = "New password for the user",
      example = "newPassword123!",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String newPassword;

  @Pattern(
      regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>_-]{8,}$",
      message =
          "Password must be at least 8 characters long and contain only letters, digits or special characters")
  @Schema(
      description = "Confirmation of the new password",
      example = "newPassword123!",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String confirmPassword;

  public void validate() {
    if (!newPassword.equals(confirmPassword)) {
      throw new IllegalArgumentException("New password and confirmation do not match");
    }
  }
}
