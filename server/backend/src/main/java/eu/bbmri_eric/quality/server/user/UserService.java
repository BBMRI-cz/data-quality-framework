package eu.bbmri_eric.quality.server.user;

public interface UserService {
  UserDTO createUser(UserCreateDTO userCreateDTO);

  /**
   * Find a user by subject ID, or create a new user if not found.
   *
   * @param subjectId the OIDC subject ID
   * @param username the username to use if creating a new user
   * @return the found or created user
   */
  UserDTO findOrCreateUserBySubjectId(String subjectId, String username);

  /**
   * Change the password of a user. Users can only change their own password.
   *
   * @param userId the ID of the user whose password should be changed
   * @param passwordChangeRequest dto containing current password, new password, and confirmation
   */
  void changePassword(Long userId, PasswordChangeRequest passwordChangeRequest);
}
