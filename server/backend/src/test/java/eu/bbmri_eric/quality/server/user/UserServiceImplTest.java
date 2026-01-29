package eu.bbmri_eric.quality.server.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for UserServiceImpl focusing on the updateUsername method. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;

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
}
