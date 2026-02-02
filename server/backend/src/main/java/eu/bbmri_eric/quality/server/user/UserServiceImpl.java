package eu.bbmri_eric.quality.server.user;

import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  private static final String CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
  private static final int DEFAULT_PASSWORD_LENGTH = 12;
  private final SecureRandom secureRandom = new SecureRandom();

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ModelMapper modelMapper;
  private final AuthenticationContextService authenticationContextService;

  UserServiceImpl(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      ModelMapper modelMapper,
      AuthenticationContextService authenticationContextService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.modelMapper = modelMapper;
    this.authenticationContextService = authenticationContextService;
  }

  @Override
  public List<UserDTO> getAllUsers() {
    return userRepository.findAll().stream()
        .map(user -> modelMapper.map(user, UserDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public UserDTO findById(Long userId) {
    return userRepository
        .findById(userId)
        .map(user -> modelMapper.map(user, UserDTO.class))
        .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
  }

  @Override
  public UserDTO createUser(UserCreateDTO userCreateDTO) {
    String rawPassword = generateRandomPassword();
    String encodedPassword = passwordEncoder.encode(rawPassword);
    User user = new User(userCreateDTO.getUsername(), encodedPassword, userCreateDTO.getAgentId());
    UserDTO savedUser = modelMapper.map(userRepository.save(user), UserDTO.class);
    savedUser.setTemporaryPassword(rawPassword);
    return savedUser;
  }

  @Override
  public UserDTO findBySubjectId(String subjectId) {
    return userRepository
        .findBySubjectId(subjectId)
        .map(user -> modelMapper.map(user, UserDTO.class))
        .orElseThrow(
            () -> new UserNotFoundException("User not found with subject ID: " + subjectId));
  }

  @Override
  @Transactional
  public UserDTO createBySubjectId(String subjectId, String username) {
    User user = new User(username, subjectId);
    user.addRole(UserRole.ADMIN);
    user.addRole(UserRole.HUMAN_USER);
    User savedUser = userRepository.save(user);
    return modelMapper.map(savedUser, UserDTO.class);
  }

  @Override
  @Transactional
  public void updateUsername(String subjectId, String newUsername) {
    if (subjectId == null || subjectId.isBlank()) {
      throw new IllegalArgumentException("Subject ID cannot be null or blank");
    }

    if (newUsername == null || newUsername.isBlank()) {
      throw new IllegalArgumentException("Username cannot be null or blank");
    }

    User user =
        userRepository
            .findBySubjectId(subjectId)
            .orElseThrow(
                () -> new UserNotFoundException("User not found with subject ID: " + subjectId));

    if (!user.getUsername().equals(newUsername)) {
      user.updateUsername(newUsername);
      userRepository.save(user);
    }
  }

  @Override
  @Transactional
  public void changePassword(Long userId, PasswordChangeRequest passwordChangeRequest) {
    Objects.requireNonNull(userId, "User ID cannot be null");
    Objects.requireNonNull(passwordChangeRequest, "Password change request cannot be null");
    passwordChangeRequest.validate();
    UserDTO currentUserDTO = authenticationContextService.getCurrentUser();
    String username = currentUserDTO.getUsername();
    Long currentUserId = currentUserDTO.getId();
    if (!currentUserId.equals(userId)) {
      throw new AccessDeniedException("You can only change your own password");
    }
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }
    String encodedPassword = passwordEncoder.encode(passwordChangeRequest.getNewPassword());
    user.setPassword(encodedPassword);
  }

  private String generateRandomPassword() {
    return generateRandomPassword(DEFAULT_PASSWORD_LENGTH);
  }

  private String generateRandomPassword(int length) {
    if (length < 4) {
      throw new IllegalArgumentException("Password length must be at least 4 characters");
    }

    StringBuilder password = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      int randomIndex = secureRandom.nextInt(CHARACTERS.length());
      password.append(CHARACTERS.charAt(randomIndex));
    }

    return password.toString();
  }
}
