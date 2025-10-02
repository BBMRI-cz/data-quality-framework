package eu.bbmri_eric.quality.agent.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "User Management", description = "APIs for user authentication and password management")
public class UserController {

  private final UserDetailService userDetailService;
  private final CurrentUserUtil currentUserUtil;

  public UserController(UserDetailService userDetailService, CurrentUserUtil currentUserUtil) {
    this.userDetailService = userDetailService;
    this.currentUserUtil = currentUserUtil;
  }

  @Operation(
      summary = "Get current user information",
      description =
          "Retrieves information about the currently authenticated user including default password status and user ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved user information",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
      })
  @GetMapping("/api/login")
  public UserDTO login(Principal principal) {
    if (Objects.isNull(principal)) {
      throw new AuthenticationCredentialsNotFoundException("No credentials found");
    }

    String username = principal.getName();
    boolean isUsingDefault = userDetailService.isUsingDefaultPassword(username);
    Long userId = userDetailService.getUserId(username);

    return new UserDTO(username, isUsingDefault, userId);
  }

  @Operation(
      summary = "Change user password",
      description =
          "Changes the password for a specific user. Users can only change their own password. Requires valid current password and new password confirmation.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password successfully changed",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class, example = "true"))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid password format, passwords do not match, or current password is incorrect",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class, example = "false"))),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class, example = "false"))),
        @ApiResponse(
            responseCode = "403",
            description =
                "User not authorized to change this password (trying to change another user's password)",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class, example = "false"))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class, example = "false")))
      })
  @PutMapping("/api/users/{userId}/password")
  public ResponseEntity<Boolean> changePassword(
      @Parameter(
              description =
                  "ID of the user whose password should be changed. Must match the current user's ID.",
              required = true,
              example = "1")
          @PathVariable
          Long userId,
      @Parameter(
              description =
                  "Password change request containing current password, new password, and confirmation",
              required = true)
          @Valid
          @RequestBody
          PasswordChangeRequest request) {
    try {
      String currentUsername = currentUserUtil.getCurrentUsername();
      if (currentUsername == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
      }

      Long currentUserId = userDetailService.getUserId(currentUsername);
      if (!currentUserId.equals(userId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
      }

      boolean success = userDetailService.changePassword(currentUsername, request);
      return ResponseEntity.ok(success);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
    } catch (UsernameNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
    }
  }
}
