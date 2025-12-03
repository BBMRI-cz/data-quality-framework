package eu.bbmri_eric.quality.agent.user;

import eu.bbmri_eric.quality.agent.user.dto.PasswordChangeRequest;

public interface UserService {
  /**
   * Change the password of a user. Users can only change their own password.
   *
   * @param userId the ID of the user whose password should be changed
   * @param passwordChangeRequest dto containing current password, new password, and confirmation
   */
  void changePassword(Long userId, PasswordChangeRequest passwordChangeRequest);
}
