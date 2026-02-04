package eu.bbmri_eric.quality.server.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "User Management", description = "APIs for user authentication and password management")
public class UserController {

  private final UserService userService;
  private final UserLinkBuilder linkBuilder;

  public UserController(UserService userService, UserLinkBuilder linkBuilder) {
    this.userService = userService;
    this.linkBuilder = linkBuilder;
  }

  @Operation(summary = "Get all users", description = "Retrieves all users in the system.")
  @GetMapping("/api/v1/users")
  public ResponseEntity<CollectionModel<EntityModel<UserDTO>>> findAll() {
    List<UserDTO> users = userService.findAll();
    CollectionModel<EntityModel<UserDTO>> userModels = linkBuilder.toCollectionModel(users);
    return ResponseEntity.ok(CollectionModel.of(userModels));
  }

  @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their ID.")
  @GetMapping("/api/v1/users/{userId}")
  public ResponseEntity<EntityModel<UserDTO>> getUserById(
      @Parameter(description = "ID of the user to retrieve", required = true, example = "1")
          @PathVariable
          Long userId) {
    UserDTO userDTO = userService.findById(userId);
    return ResponseEntity.ok(linkBuilder.toModel(userDTO));
  }

  @Operation(
      summary = "Change user password",
      description = "Changes the password for a specific user.")
  @PutMapping("/api/users/{userId}/password")
  public ResponseEntity<Void> changePassword(
      @Parameter(
              description = "ID of the user whose password should be changed.",
              required = true,
              example = "1")
          @PathVariable
          Long userId,
      @Parameter(description = "Password change request", required = true) @Valid @RequestBody
          PasswordChangeRequest request) {
    userService.changePassword(userId, request);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Get current user",
      description = "Retrieves information about the currently authenticated user.")
  @GetMapping("/api/userinfo")
  public ResponseEntity<EntityModel<UserDTO>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
      return ResponseEntity.status(401).build();
    }
    String subjectId = jwt.getSubject();
    UserDTO userDTO = userService.findBySubjectId(subjectId);
    return ResponseEntity.ok(linkBuilder.toModel(userDTO));
  }
}
