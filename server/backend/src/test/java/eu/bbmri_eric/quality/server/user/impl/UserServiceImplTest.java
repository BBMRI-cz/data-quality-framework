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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;

/** Unit tests for UserServiceImpl. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private ModelMapper modelMapper;
  @Mock private AuthenticationContextService authenticationContextService;
  @Mock private EntityManager entityManager;

  @Mock private ModelMapper modelMapper;

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
  @DisplayName("createBySubjectId_assignsOnlyHumanUserRole")
  void createBySubjectId_assignsOnlyHumanUserRole() {
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(modelMapper.map(any(User.class), eq(UserDTO.class)))
        .thenReturn(new UserDTO(TEST_USERNAME, 1L));

    userService.createBySubjectId(TEST_SUBJECT_ID, TEST_USERNAME);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User savedUser = captor.getValue();
    assertEquals(TEST_SUBJECT_ID, savedUser.getSubjectId());
    assertEquals(TEST_USERNAME, savedUser.getUsername());
    assertEquals(Set.of(UserRole.HUMAN_USER), savedUser.getRoles());
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
    User admin = adminUser();
    User human = new User("human", "subject-human");
    human.addRole(UserRole.HUMAN_USER);
    when(authenticationContextService.getCurrentUser()).thenReturn(new UserDTO("admin", 1L));
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.findById(2L)).thenReturn(Optional.of(human));
    when(userRepository.save(human)).thenReturn(human);
    when(modelMapper.map(human, UserDTO.class)).thenReturn(new UserDTO("human", 2L));

    userService.updateRoles(2L, Set.of(UserRole.HUMAN_USER, UserRole.ADMIN));

    assertEquals(Set.of(UserRole.HUMAN_USER, UserRole.ADMIN), human.getRoles());
    verify(userRepository).save(human);
  }

  @Test
  @DisplayName("updateRoles_locksActorAndTargetInIdOrder")
  void updateRoles_locksActorAndTargetInIdOrder() {
    User admin = adminUser();
    User human = new User("human", "subject-human");
    human.addRole(UserRole.HUMAN_USER);
    when(authenticationContextService.getCurrentUser()).thenReturn(new UserDTO("admin", 5L));
    when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
    when(userRepository.findById(2L)).thenReturn(Optional.of(human));
    when(userRepository.save(human)).thenReturn(human);

    userService.updateRoles(2L, Set.of(UserRole.HUMAN_USER));

    var inOrder = inOrder(entityManager);
    inOrder.verify(entityManager).refresh(human, LockModeType.PESSIMISTIC_WRITE);
    inOrder.verify(entityManager).refresh(admin, LockModeType.PESSIMISTIC_WRITE);
  }

  @Test
  @DisplayName("updateRoles_whenActorNoLongerAdmin_throwsAccessDeniedException")
  void updateRoles_whenActorNoLongerAdmin_throwsAccessDeniedException() {
    User demoted = new User("admin", "subject-admin");
    demoted.addRole(UserRole.HUMAN_USER);
    when(authenticationContextService.getCurrentUser()).thenReturn(new UserDTO("admin", 1L));
    when(userRepository.findById(1L)).thenReturn(Optional.of(demoted));

    assertThrows(
        AccessDeniedException.class,
        () -> userService.updateRoles(1L, Set.of(UserRole.HUMAN_USER, UserRole.ADMIN)));

    assertEquals(Set.of(UserRole.HUMAN_USER), demoted.getRoles());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateRoles_removingOwnAdminRole_throwsIllegalArgumentException")
  void updateRoles_removingOwnAdminRole_throwsIllegalArgumentException() {
    User admin = adminUser();
    when(authenticationContextService.getCurrentUser()).thenReturn(new UserDTO("admin", 1L));
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

    assertThrows(
        IllegalArgumentException.class,
        () -> userService.updateRoles(1L, Set.of(UserRole.HUMAN_USER)));

    assertTrue(admin.hasRole(UserRole.ADMIN));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("findAllRoles_returnsEveryRole")
  void findAllRoles_returnsEveryRole() {
    assertEquals(List.of(UserRole.values()), userService.findAllRoles());
  }

  @Test
  @DisplayName("updateRoles_withNonExistentUser_throwsUserNotFoundException")
  void updateRoles_withNonExistentUser_throwsUserNotFoundException() {
    when(authenticationContextService.getCurrentUser()).thenReturn(new UserDTO("admin", 1L));
    when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
    when(userRepository.findById(42L)).thenReturn(Optional.empty());

    assertThrows(
        UserNotFoundException.class, () -> userService.updateRoles(42L, Set.of(UserRole.ADMIN)));
  }

  private User adminUser() {
    User admin = new User("admin", "subject-admin");
    admin.addRole(UserRole.HUMAN_USER);
    admin.addRole(UserRole.ADMIN);
    return admin;
  }
}
