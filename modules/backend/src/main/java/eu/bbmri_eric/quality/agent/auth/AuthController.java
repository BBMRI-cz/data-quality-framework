package eu.bbmri_eric.quality.agent.auth;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @GetMapping("/check")
  public Map<String, Object> check(Authentication auth) {
    return Map.of(
        "username", auth.getName(),
        "roles",
            auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList()));
  }
}
