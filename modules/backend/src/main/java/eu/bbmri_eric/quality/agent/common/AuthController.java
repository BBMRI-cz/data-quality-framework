package eu.bbmri_eric.quality.agent.common;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @GetMapping("/login")
  public Map<String, Object> check(Authentication auth) {
    String username = null;

    if (auth != null) {
      Object principal = auth.getPrincipal();
      if (principal instanceof UserDetails u) {
        username = u.getUsername();
      } else {
        username = auth.getName();
      }
    }

    return Map.of("username", username);
  }
}
