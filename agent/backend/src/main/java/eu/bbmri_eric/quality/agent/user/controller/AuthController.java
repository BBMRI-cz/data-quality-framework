package eu.bbmri_eric.quality.agent.user.controller;

import eu.bbmri_eric.quality.agent.user.AuthenticationContextService;
import eu.bbmri_eric.quality.agent.user.dto.LoginRequest;
import eu.bbmri_eric.quality.agent.user.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for authentication operations: JWT token generation on login, and logout. */
@RestController
@Tag(name = "Authentication", description = "Authentication management endpoints")
class AuthController {

  private final AuthenticationContextService authenticationContextService;

  AuthController(AuthenticationContextService authenticationContextService) {
    this.authenticationContextService = authenticationContextService;
  }

  /**
   * Authenticates user credentials and returns JWT token.
   *
   * @param loginRequest containing username and password
   * @return LoginResponse with JWT token and user information
   */
  @Operation(
      summary = "Authenticate user",
      description =
          "Authenticates user credentials and returns a JWT token for subsequent API calls")
  @PostMapping("/api/auth/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    return ResponseEntity.ok(authenticationContextService.login(loginRequest));
  }

  /**
   * Logs out the current user. Tokens are stateless, so the client must discard its token; this
   * only records the logout in the audit log.
   */
  @Operation(
      summary = "Log out user",
      description = "Records the logout of the current user; the client must discard its token")
  @PostMapping("/api/auth/logout")
  public ResponseEntity<Void> logout() {
    authenticationContextService.logout();
    return ResponseEntity.noContent().build();
  }
}
