package eu.bbmri_eric.quality.agent.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BasicAuthSecurityIntegrationTests.TestWebConfig.class)
class BasicAuthSecurityIntegrationTests {

  @Autowired private MockMvc mvc;

  @Test
  void options_isPermitted_andCorsHeadersPresent() throws Exception {
    mvc.perform(
            options("/api/ping")
                .header("Origin", "http://example.com")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://example.com"))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
        .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
        .andExpect(header().string("Access-Control-Max-Age", "3600"));
  }

  @Test
  void get_requiresAuth() throws Exception {
    mvc.perform(get("/api/ping")).andExpect(status().isUnauthorized());
  }

  @Test
  void get_withValidBasicAuth_isOk() throws Exception {
    mvc.perform(get("/api/ping").with(httpBasic("test", "pass")))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
  }

  @Test
  void post_withoutCsrf_butWithBasicAuth_isOk_becauseCsrfDisabled() throws Exception {
    mvc.perform(post("/api/ping").with(httpBasic("test", "pass")))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
  }

  @Test
  void post_withoutAuth_isUnauthorized() throws Exception {
    mvc.perform(post("/api/ping")).andExpect(status().isUnauthorized());
  }

  @TestConfiguration
  @Import(TestController.class)
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

  @RestController
  static class TestController {
    @GetMapping("/api/ping")
    public String get() {
      return "ok";
    }

    @PostMapping("/api/ping")
    public String post() {
      return "ok";
    }
  }
}
