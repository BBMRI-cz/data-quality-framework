package eu.bbmri_eric.quality.agent.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.common.AuthController;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootTest
class BasicAuthSecurityTest {

  @Autowired private SecurityFilterChain securityFilterChain;

  @Autowired private PasswordEncoder passwordEncoder;

  private final AuthController controller = new AuthController();

  @Test
  void exposesSecurityFilterChainBean() {
    assertThat(securityFilterChain).isNotNull();
  }

  @Test
  void passwordEncoder_encodesAndMatches() {
    String raw = "pass";
    String encoded = passwordEncoder.encode(raw);
    assertThat(encoded).isNotBlank();
    assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
  }

  @Test
  void check_withUserDetailsPrincipal_returnsUsernameAndId() {
    UserDetails principal = User.withUsername("test").password("x").build();
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(principal);

    Map<String, Object> body = controller.check(auth);

    assertThat(body.get("username")).isEqualTo("test");
  }
}
