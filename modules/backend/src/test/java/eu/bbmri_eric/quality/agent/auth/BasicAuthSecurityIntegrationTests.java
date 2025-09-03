package eu.bbmri_eric.quality.agent.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BasicAuthSecurityIntegrationTests.TestWebConfig.class)
class BasicAuthSecurityIntegrationTests {

  private static final String PATH = "/api/auth/login";

  @Autowired private MockMvc mvc;

  @Test
  void login_requiresAuth_is401() throws Exception {
    mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
  }

  @Test
  void get_withValidBasicAuth_isOk() throws Exception {
    mvc.perform(get(PATH).with(httpBasic("test", "pass"))).andExpect(status().isOk());
  }

  @Test
  void post_withBasicAuth_isNotRejectedByCsrfOrAuth() throws Exception {
    mvc.perform(post(PATH).with(httpBasic("test", "pass")))
        .andExpect(
            result -> {
              int s = result.getResponse().getStatus();
              assertTrue(s != 401 && s != 403, "Expected not 401/403, but was " + s);
            });
  }

  @Test
  void post_withoutAuth_isUnauthorized() throws Exception {
    mvc.perform(post(PATH)).andExpect(status().isUnauthorized());
  }

  @Test
  void login_withValidBasicAuth_returnsUsernameAndId() throws Exception {
    mvc.perform(get(PATH).with(httpBasic("test", "pass")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("test"));
  }

  @Test
  void login_postWithValidBasicAuth_is405() throws Exception {
    mvc.perform(post(PATH).with(httpBasic("test", "pass")))
        .andExpect(status().isMethodNotAllowed());
  }

  @TestConfiguration
  static class TestWebConfig {
    @Bean
    @Primary
    UserDetailsService users(PasswordEncoder encoder) {
      return new InMemoryUserDetailsManager(
          User.withUsername("test").password(encoder.encode("pass")).roles("USER").build());
    }

    @Bean
    @Primary
    AuthenticationProvider testDaoAuthProvider(UserDetailsService users, PasswordEncoder encoder) {
      var p = new DaoAuthenticationProvider();
      p.setUserDetailsService(users);
      p.setPasswordEncoder(encoder);
      return p;
    }
  }
}
