package eu.bbmri_eric.quality.server.user.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.quality.server.user.AuthenticationContextService;
import eu.bbmri_eric.quality.server.user.UserRole;
import eu.bbmri_eric.quality.server.user.domain.User;
import eu.bbmri_eric.quality.server.user.dto.UserDTO;
import eu.bbmri_eric.quality.server.user.exception.UserNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

/** Unit tests for UserServiceImpl focusing on the updateUsername method. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private ModelMapper modelMapper;
  @Mock private AuthenticationContextService authenticationContextService;

  @InjectMocks private UserServiceImpl userService;

  private User testUser;
  private static final String TEST_SUBJECT_ID = "test1";
  private static final String TEST_USERNAME = "testUser";
  private static final String NEW_USERNAME = "newTestUser";
  private static final String AGENT_ID = "agent-001";

  @BeforeEach
  void setUp() {
    testUser = new User(TEST_USERNAME, TEST_SUBJECT_ID);
    testUser.setAgentId(AGENT_ID);
  }

  @Test
  @DisplayName("updateUsername_withValidData_updatesUsername")
  void updateUsername_withValidData_updatesUsername() {
    when(userRepository.findBySubjectId(TEST_SUBJECT_ID)).thenReturn(Optional.of(testUser));
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    userService.updateUsername(TEST_SUBJECT_ID, NEW_USERNAME);

    verify(userRepository).findBySubjectId(TEST_SUBJECT_ID);
    verify(userRepository).save(testUser);
    assertEquals(NEW_USERNAME, testUser.getUsername());
  }

  @Test
  @DisplayName("updateUsername_withSameUsername_doesNotSave")
  void updateUsername_withSameUsername_doesNotSave() {
    when(userRepository.findBySubjectId(TEST_SUBJECT_ID)).thenReturn(Optional.of(testUser));

    userService.updateUsername(TEST_SUBJECT_ID, TEST_USERNAME);

    verify(userRepository).findBySubjectId(TEST_SUBJECT_ID);
    verify(userRepository, never()).save(any(User.class));
    assertEquals(TEST_USERNAME, testUser.getUsername());
  }

  @Test
  @DisplayName("updateUsername_withNullSubjectId_throwsIllegalArgumentException")
  void updateUsername_withNullSubjectId_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> userService.updateUsername(null, NEW_USERNAME));

    assertEquals("Subject ID cannot be null or blank", exception.getMessage());
    verify(userRepository, never()).findBySubjectId(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateUsername_withWhitespaceSubjectId_throwsIllegalArgumentException")
  void updateUsername_withWhitespaceSubjectId_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> userService.updateUsername("   ", NEW_USERNAME));

    assertEquals("Subject ID cannot be null or blank", exception.getMessage());
    verify(userRepository, never()).findBySubjectId(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateUsername_withNullUsername_throwsIllegalArgumentException")
  void updateUsername_withNullUsername_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUsername(TEST_SUBJECT_ID, null));

    assertEquals("Username cannot be null or blank", exception.getMessage());
    verify(userRepository, never()).findBySubjectId(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateUsername_withBlankUsername_throwsIllegalArgumentException")
  void updateUsername_withBlankUsername_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> userService.updateUsername(TEST_SUBJECT_ID, ""));

    assertEquals("Username cannot be null or blank", exception.getMessage());
    verify(userRepository, never()).findBySubjectId(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateUsername_withNonExistentUser_throwsUserNotFoundException")
  void updateUsername_withNonExistentUser_throwsUserNotFoundException() {
    when(userRepository.findBySubjectId(TEST_SUBJECT_ID)).thenReturn(Optional.empty());

    UserNotFoundException exception =
        assertThrows(
            UserNotFoundException.class,
            () -> userService.updateUsername(TEST_SUBJECT_ID, NEW_USERNAME));

    assertEquals("User not found with subject ID: " + TEST_SUBJECT_ID, exception.getMessage());
    verify(userRepository).findBySubjectId(TEST_SUBJECT_ID);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateRoles_forOtherHumanUser_replacesRoles")
  void updateRoles_forOtherHumanUser_replacesRoles() {
    User human = new User("human", "subject-human");
    human.addRole(UserRole.HUMAN_USER);
    when(userRepository.findById(2L)).thenReturn(Optional.of(human));
    when(authenticationContextService.getCurrentUser()).thenReturn(new UserDTO("admin", 1L));
    when(userRepository.save(human)).thenReturn(human);
    when(modelMapper.map(human, UserDTO.class)).thenReturn(new UserDTO("human", 2L));

    userService.updateRoles(2L, Set.of(UserRole.HUMAN_USER, UserRole.ADMIN));

    assertEquals(Set.of(UserRole.HUMAN_USER, UserRole.ADMIN), human.getRoles());
    verify(userRepository).save(human);
  }

  @Test
  @DisplayName("updateRoles_removingOwnAdminRole_throwsIllegalArgumentException")
  void updateRoles_removingOwnAdminRole_throwsIllegalArgumentException() {
    User admin = new User("admin", "subject-admin");
    admin.addRole(UserRole.ADMIN);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(authenticationContextService.getCurrentUser()).thenReturn(new UserDTO("admin", 1L));

    assertThrows(
        IllegalArgumentException.class,
        () -> userService.updateRoles(1L, Set.of(UserRole.HUMAN_USER)));

    assertTrue(admin.hasRole(UserRole.ADMIN));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateRoles_withoutHumanUserRole_throwsIllegalArgumentException")
  void updateRoles_withoutHumanUserRole_throwsIllegalArgumentException() {
    User human = new User("human", "subject-human");
    human.addRole(UserRole.HUMAN_USER);
    when(userRepository.findById(2L)).thenReturn(Optional.of(human));

    assertThrows(
        IllegalArgumentException.class, () -> userService.updateRoles(2L, Set.of(UserRole.ADMIN)));

    assertEquals(Set.of(UserRole.HUMAN_USER), human.getRoles());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateRoles_forAgentAccount_throwsIllegalArgumentException")
  void updateRoles_forAgentAccount_throwsIllegalArgumentException() {
    when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));

    assertThrows(
        IllegalArgumentException.class, () -> userService.updateRoles(3L, Set.of(UserRole.ADMIN)));

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateRoles_withEmptyRoles_throwsIllegalArgumentException")
  void updateRoles_withEmptyRoles_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> userService.updateRoles(1L, Set.of()));

    verify(userRepository, never()).findById(any());
  }

  @Test
  @DisplayName("findAllRoles_returnsEveryRole")
  void findAllRoles_returnsEveryRole() {
    assertEquals(List.of(UserRole.values()), userService.findAllRoles());
  }

  @Test
  @DisplayName("updateRoles_withNonExistentUser_throwsUserNotFoundException")
  void updateRoles_withNonExistentUser_throwsUserNotFoundException() {
    when(userRepository.findById(42L)).thenReturn(Optional.empty());

    assertThrows(
        UserNotFoundException.class, () -> userService.updateRoles(42L, Set.of(UserRole.ADMIN)));
  }
}
