package eu.bbmri_eric.quality.agent.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request object for changing user password")
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

  public PasswordChangeRequest(String currentPassword, String newPassword, String confirmPassword) {
    this.currentPassword = currentPassword;
    this.newPassword = newPassword;
    this.confirmPassword = confirmPassword;
  }

  public String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  public String getConfirmPassword() {
    return confirmPassword;
  }

  public void setConfirmPassword(String confirmPassword) {
    this.confirmPassword = confirmPassword;
  }

  public void validate() {
    if (!newPassword.equals(confirmPassword)) {
      throw new IllegalArgumentException("New password and confirmation do not match");
    }
  }
}
