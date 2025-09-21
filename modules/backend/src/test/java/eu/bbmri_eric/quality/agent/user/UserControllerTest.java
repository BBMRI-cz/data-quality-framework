package eu.bbmri_eric.quality.agent.user;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

  private static final String LOGIN_ENDPOINT = "/api/login";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";
  private static final String OTHER_USER = "user";

  @Autowired private MockMvc mockMvc;

  @Test
  void login_correctAuth_ok() throws Exception {
    mockMvc
        .perform(get(LOGIN_ENDPOINT).with(httpBasic(ADMIN_USER, ADMIN_PASS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is(ADMIN_USER)));
  }

  @Test
  void login_wrongPassword_unauthorized() throws Exception {
    mockMvc
        .perform(get(LOGIN_ENDPOINT).with(httpBasic(OTHER_USER, "wrongpassword")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_missingAuth_unauthorized() throws Exception {
    mockMvc.perform(get(LOGIN_ENDPOINT)).andExpect(status().isUnauthorized());
  }
}
