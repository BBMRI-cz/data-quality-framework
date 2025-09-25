package eu.bbmri_eric.quality.agent.user;

import jakarta.validation.constraints.Pattern;

class PasswordChangeRequest {
  private String currentPassword;

  @Pattern(
      regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>_-]{8,}$",
      message =
          "Password must be at least 8 characters long and contain only letters, digits or special characters")
  private String newPassword;

  @Pattern(
      regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>_-]{8,}$",
      message =
          "Password must be at least 8 characters long and contain only letters, digits or special characters")
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
