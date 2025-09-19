package eu.bbmri_eric.quality.agent.user;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private final UserDetailService userDetailService;

  public UserController(UserDetailService userDetailService) {
    this.userDetailService = userDetailService;
  }

  @PostMapping("/change-password")
  public ResponseEntity<?> changePassword(
      @RequestBody Map<String, String> request, Authentication authentication) {

    try {
      String username = authentication.getName();
      userDetailService.changePassword(
          username,
          request.get("currentPassword"),
          request.get("newPassword"),
          request.get("confirmPassword"));

      return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));

    } catch (UsernameNotFoundException e) {
      return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));

    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("success", false, "error", "An unexpected error occurred"));
    }
  }
}
