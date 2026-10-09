package eu.bbmri_eric.quality.server.user.impl;

import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import eu.bbmri_eric.quality.server.user.AuthenticationContextService;
import eu.bbmri_eric.quality.server.user.UserRole;
import eu.bbmri_eric.quality.server.user.UserService;
import eu.bbmri_eric.quality.server.user.domain.User;
import eu.bbmri_eric.quality.server.user.dto.PasswordChangeRequest;
import eu.bbmri_eric.quality.server.user.dto.UserCreateDTO;
import eu.bbmri_eric.quality.server.user.dto.UserDTO;
import eu.bbmri_eric.quality.server.user.dto.UserFilterDTO;
import eu.bbmri_eric.quality.server.user.exception.UserNotFoundException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.NotImplementedException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

  private static final String CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
  private static final int DEFAULT_PASSWORD_LENGTH = 12;

  /** Fields of {@link User} that clients are allowed to sort by. */
  private static final List<String> SORTABLE_FIELDS =
      List.of("id", "username", "subjectId", "agentId");

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

  @Override
  public UserDTO create(UserCreateDTO userCreateDTO) {
    throw new NotImplementedException("Not yet implemented");
  }

  @Override
  public UserDTO findById(Long userId) {
    return userRepository
        .findById(userId)
        .map(user -> modelMapper.map(user, UserDTO.class))
        .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
  }

  @Override
  public List<UserDTO> findAll() {
    return userRepository.findAll().stream()
        .map(user -> modelMapper.map(user, UserDTO.class))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<UserDTO> findAll(UserFilterDTO filter) {
    FilterDTO normalizedFilter = normalizeFilter(filter);
    Sort.Direction direction =
        normalizedFilter.getOrder() == FilterDTO.SortOrder.DESC
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

    Sort sort = Sort.by(direction, normalizedFilter.getSort());
    PageRequest pageRequest =
        PageRequest.of(normalizedFilter.getPage(), normalizedFilter.getSize(), sort);
    Page<User> page = userRepository.findAll(UserSpecification.fromFilter(filter), pageRequest);

    List<UserDTO> content =
        page.getContent().stream().map(user -> modelMapper.map(user, UserDTO.class)).toList();
    return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
  }

  private FilterDTO normalizeFilter(FilterDTO filter) {
    if (filter.getOrder() == null) {
      filter.setOrder(FilterDTO.SortOrder.ASC);
    }

    if (filter.getSort() == null || filter.getSort().isBlank()) {
      filter.setSort("id");
    }

    if (!SORTABLE_FIELDS.contains(filter.getSort())) {
      throw new IllegalArgumentException(
          "Unsupported sort field: '%s'. Supported fields: %s"
              .formatted(filter.getSort(), SORTABLE_FIELDS));
    }

    return filter;
  }

  @Override
  public UserDTO update(Long id, UserDTO userDTO) {
    throw new NotImplementedException("Not yet implemented");
  }

  @Override
  public long count() {
    throw new NotImplementedException("Not yet implemented");
  }

  @Override
  public void delete(Long id) {
    throw new NotImplementedException("Not yet implemented");
  }

  @Override
  @Transactional
  public void deleteUser(String agentId) {
    userRepository.findByAgentId(agentId).ifPresent(userRepository::delete);
  }

  @Override
  public boolean exists(Long id) {
    throw new NotImplementedException("Not yet implemented");
  }
}
