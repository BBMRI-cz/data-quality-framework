package eu.bbmri_eric.quality.agent.user;

import java.security.Principal;
import java.util.Objects;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  @GetMapping("/login")
  public String login(Principal principal) {
    if (Objects.isNull(principal)) {
      throw new AuthenticationCredentialsNotFoundException("No credentials found");
    }
    return "Hello, " + principal.getName();
  }
}
