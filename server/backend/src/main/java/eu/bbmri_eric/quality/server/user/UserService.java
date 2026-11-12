package eu.bbmri_eric.quality.server.user;

import eu.bbmri_eric.quality.server.common.CRUDService;
import eu.bbmri_eric.quality.server.user.dto.PasswordChangeRequest;
import eu.bbmri_eric.quality.server.user.dto.UserCreateDTO;
import eu.bbmri_eric.quality.server.user.dto.UserDTO;
import eu.bbmri_eric.quality.server.user.dto.UserFilterDTO;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserService
    extends CRUDService<UserDTO, UserCreateDTO, UserDTO, Long, UserFilterDTO> {
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
   * @param subjectId the OIDC subject ID
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

  /**
   * Delete a user by agent ID.
   *
   * @param agentId the agent ID associated with the user
   */
  void deleteUser(String agentId);

  /**
   * Replace the roles assigned to a human user. Restricted to administrators. The HUMAN_USER role
   * must always be kept, so in practice this grants or revokes ADMIN.
   *
   * @param userId the ID of the user whose roles should be updated
   * @param roles the new set of roles to assign, replacing any existing roles
   * @return the updated user
   */
  UserDTO updateRoles(Long userId, Set<UserRole> roles);

  /**
   * List all roles that can be assigned to users.
   *
   * @return every available role
   */
  List<UserRole> findAllRoles();
}
