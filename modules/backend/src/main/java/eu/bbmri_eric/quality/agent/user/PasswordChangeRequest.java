package eu.bbmri_eric.quality.agent.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for changing user password")
public class PasswordChangeRequest {

  @Schema(description = "Current password of the user", example = "oldPassword123", required = true)
  private String currentPassword;

  @Schema(description = "New password for the user", example = "newPassword123!", required = true)
  private String newPassword;

  @Schema(
      description = "Confirmation of the new password",
      example = "newPassword123!",
      required = true)
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
}
