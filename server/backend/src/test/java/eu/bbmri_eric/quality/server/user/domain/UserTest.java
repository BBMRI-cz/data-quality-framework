package eu.bbmri_eric.quality.server.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.quality.server.user.UserRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void setRoles_withHumanUserRole_setsRoles() {
    User user = humanUser();

    user.setRoles(Set.of(UserRole.HUMAN_USER, UserRole.ADMIN));

    assertEquals(Set.of(UserRole.HUMAN_USER, UserRole.ADMIN), user.getRoles());
  }

  @Test
  void setRoles_withoutHumanUserRole_throwsAndKeepsRoles() {
    User user = humanUser();

    assertThrows(IllegalArgumentException.class, () -> user.setRoles(Set.of(UserRole.ADMIN)));

    assertEquals(Set.of(UserRole.HUMAN_USER), user.getRoles());
  }

  @Test
  void setRoles_withEmptyRoles_throwsAndKeepsRoles() {
    User user = humanUser();

    assertThrows(IllegalArgumentException.class, () -> user.setRoles(Set.of()));

    assertEquals(Set.of(UserRole.HUMAN_USER), user.getRoles());
  }

  @Test
  void setRoles_withNullRoles_throws() {
    assertThrows(IllegalArgumentException.class, () -> humanUser().setRoles(null));
  }

  @Test
  void setRoles_forAgentAccount_throwsAndKeepsRoles() {
    User agent = new User("agent-user", "encoded", "agent-001");

    assertThrows(
        IllegalArgumentException.class,
        () -> agent.setRoles(Set.of(UserRole.HUMAN_USER, UserRole.ADMIN)));

    assertEquals(Set.of(), agent.getRoles());
  }

  private User humanUser() {
    User user = new User("human", "subject-human");
    user.addRole(UserRole.HUMAN_USER);
    return user;
  }
}
