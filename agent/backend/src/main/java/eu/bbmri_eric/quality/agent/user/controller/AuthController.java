package eu.bbmri_eric.quality.agent.user.controller;

import eu.bbmri_eric.quality.agent.common.JwtUtil;
import eu.bbmri_eric.quality.agent.user.dto.CustomUserDetails;
import eu.bbmri_eric.quality.agent.user.dto.LoginRequest;
import eu.bbmri_eric.quality.agent.user.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for authentication operations. Handles JWT token generation for user login. */
@RestController
@Tag(name = "Authentication", description = "Authentication management endpoints")
class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtService;
  private final UserDetailsService userDetailService;

  AuthController(
      AuthenticationManager authenticationManager,
      JwtUtil jwtService,
      UserDetailsService userDetailService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.userDetailService = userDetailService;
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
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.username(), loginRequest.password()));
    CustomUserDetails userDetails =
        (CustomUserDetails) userDetailService.loadUserByUsername(loginRequest.username());
    return ResponseEntity.ok(
        new LoginResponse(jwtService.generateToken(authentication), userDetails.getUser()));
  }
}
