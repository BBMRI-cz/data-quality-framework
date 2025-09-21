package eu.bbmri_eric.quality.agent.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  private String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
    return "Basic " + encoded;
  }

  @Test
  void login_correctAuth_ok() throws Exception {
    mockMvc
        .perform(get("/login").header(HttpHeaders.AUTHORIZATION, basicAuth("admin", "adminpass")))
        .andExpect(status().isOk())
        .andExpect(content().string("Hello, admin"));
  }

  @Test
  void login_wrongPassword_unauthorized() throws Exception {
    mockMvc
        .perform(
            get("/login").header(HttpHeaders.AUTHORIZATION, basicAuth("user", "wrongpassword")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_missingAuth_unauthorized() throws Exception {
    mockMvc.perform(get("/login")).andExpect(status().isUnauthorized());
  }
}
