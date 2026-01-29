package eu.bbmri_eric.quality.server.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(
      summary = "Change user password",
      description = "Changes the password for a specific user.")
  @PutMapping("/api/users/{userId}/password")
  public void changePassword(
      @Parameter(
              description = "ID of the user whose password should be changed.",
              required = true,
              example = "1")
          @PathVariable
          Long userId,
      @Parameter(description = "Password change request", required = true) @Valid @RequestBody
          PasswordChangeRequest request) {
    userService.changePassword(userId, request);
  }

  @Operation(
      summary = "Get current user",
      description = "Retrieves information about the currently authenticated user.")
  @GetMapping("/api/userinfo")
  public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
      return ResponseEntity.status(401).build();
    }
    String subjectId = jwt.getSubject();

    UserDTO userDTO = userService.findBySubjectId(subjectId);
    return ResponseEntity.ok(userDTO);
  }
}
