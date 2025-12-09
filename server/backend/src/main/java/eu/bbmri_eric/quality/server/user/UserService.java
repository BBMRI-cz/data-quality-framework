package eu.bbmri_eric.quality.server.user;

import java.util.Optional;

public interface UserService {
  UserDTO createUser(UserCreateDTO userCreateDTO);

  /**
   * Find a user by subject ID,
   *
   * @param subjectId the OIDC subject ID
   * @return the found or created user
   */
  Optional<UserDTO> findBySubjectId(String subjectId, String username);

  UserDTO createBySubjectId(String subjectId, String username);

  /**
   * Change the password of a user. Users can only change their own password.
   *
   * @param userId the ID of the user whose password should be changed
   * @param passwordChangeRequest dto containing current password, new password, and confirmation
   */
  void changePassword(Long userId, PasswordChangeRequest passwordChangeRequest);
}
