package eu.bbmri_eric.quality.server.user.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.quality.server.user.UserRole;
import eu.bbmri_eric.quality.server.user.UserService;
import eu.bbmri_eric.quality.server.user.domain.User;
import eu.bbmri_eric.quality.server.user.dto.UserDTO;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration tests for {@link UserServiceImpl} against a real database. */
@IntegrationTest
class UserServiceImplIntegrationTest {

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("createBySubjectId creates a new OIDC user with default roles")
  void createBySubjectId_withNewUser_persistsUserWithDefaultRoles() {
    UserDTO created = userService.createBySubjectId("sub-integration-1", "Alice Researcher");

    assertNotNull(created.getId());
    assertEquals("sub-integration-1", created.getSubjectId());
    assertTrue(created.getRoles().contains(UserRole.ADMIN));
    assertTrue(created.getRoles().contains(UserRole.HUMAN_USER));
  }

  @Test
  @DisplayName(
      "createBySubjectId is idempotent - concurrent first logins return the existing user"
          + " without a constraint violation")
  void createBySubjectId_withExistingUser_returnsExistingUserWithoutError() {
    String subjectId = "sub-integration-2";
    String username = "Bob Researcher";

    // Simulates the concurrent first-OIDC-login race: the second creation attempt must not
    // raise a unique constraint violation but return the already created user.
    UserDTO created = userService.createBySubjectId(subjectId, username);
    UserDTO raced = assertDoesNotThrow(() -> userService.createBySubjectId(subjectId, username));

    assertEquals(created.getId(), raced.getId());
    assertEquals(
        1,
        userRepository.findAll().stream()
            .filter(user -> username.equals(user.getUsername()))
            .count());
    User stored = userRepository.findBySubjectId(subjectId).orElseThrow();
    assertTrue(stored.hasRole(UserRole.HUMAN_USER));
    assertTrue(stored.hasRole(UserRole.ADMIN));
  }
}
