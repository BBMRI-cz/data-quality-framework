package eu.bbmri_eric.quality.agent.user;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  private final UserDetailService userDetailService;

  public UserController(UserDetailService userDetailService) {
    this.userDetailService = userDetailService;
  }

  @GetMapping("/api/login")
  public UserDTO login(Principal principal) {
    if (Objects.isNull(principal)) {
      throw new AuthenticationCredentialsNotFoundException("No credentials found");
    }
    return new UserDTO(principal.getName());
  }

  @PutMapping("/api/user/password")
  public ResponseEntity<PasswordChangeResponse> changePassword(
      @Valid @RequestBody PasswordChangeRequest request,
      BindingResult bindingResult,
      Principal principal) {
    PasswordChangeResponse response =
        userDetailService.changePassword(principal, request, bindingResult);
    if (response.getSuccess()) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.badRequest().body(response);
    }
  }
}
