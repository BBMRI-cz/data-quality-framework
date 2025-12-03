package eu.bbmri_eric.quality.agent.user.controller;

import eu.bbmri_eric.quality.agent.user.UserService;
import eu.bbmri_eric.quality.agent.user.dto.PasswordChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "User Management", description = "APIs for user authentication and password management")
class UserController {

  private final UserService userService;

  UserController(UserService userService) {
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
}
