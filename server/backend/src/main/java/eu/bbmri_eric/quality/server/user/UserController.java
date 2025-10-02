package eu.bbmri_eric.quality.server.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user-related operations.
 * Follows REST principles with proper resource naming and HTTP methods.
 */
@RestController
public class UserController {

  private final AuthenticationContextService authenticationContextService;

  public UserController(AuthenticationContextService authenticationContextService) {
    this.authenticationContextService = authenticationContextService;
  }

  /**
   * Get current authenticated user profile information.
   *
   * @return current user DTO
   */
  @GetMapping("/api/auth/login")
  public ResponseEntity<UserDTO> getUserProfile() {
    return ResponseEntity.ok(authenticationContextService.getCurrentUser());
  }
}
