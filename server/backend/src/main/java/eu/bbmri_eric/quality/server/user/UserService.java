package eu.bbmri_eric.quality.server.user;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserService {
  UserDTO createUser(UserCreateDTO userCreateDTO);

  /**
   * Find a user by subject ID.
   *
   * @param subjectId the OIDC subject ID
   * @return the found user
   * @throws UsernameNotFoundException if user is not found
   */
  UserDTO findBySubjectId(String subjectId);

  UserDTO createBySubjectId(String subjectId, String username);

  /**
   * Update username for a user identified by subject ID if it has changed.
   *
   * @param subjectId   the OIDC subject ID
   * @param newUsername the new username from OIDC userinfo
   */
  void updateUsername(String subjectId, String newUsername);

  /**
   * Change the password of a user. Users can only change their own password.
   *
   * @param userId the ID of the user whose password should be changed
   * @param passwordChangeRequest dto containing current password, new password, and confirmation
   */
  void changePassword(Long userId, PasswordChangeRequest passwordChangeRequest);
}
