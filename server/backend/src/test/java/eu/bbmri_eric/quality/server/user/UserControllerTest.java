package eu.bbmri_eric.quality.server.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for UserController using MockMvc with @SpringBootTest.
 * Tests the /api/auth/login endpoint with real Spring Security and database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

  private static final String LOGIN_ENDPOINT = "/api/auth/login";
  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "adminpass";
  @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Should be case sensitive for username")
    void login_withDifferentCase_returnsUnauthorized() throws Exception {
      mockMvc.perform(get(LOGIN_ENDPOINT)
              .with(httpBasic(ADMIN_USERNAME.toUpperCase(), ADMIN_PASSWORD)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return OK for successful login with correct credentials")
    void login_withCorrectCredentials_returnsOk() throws Exception {
      mockMvc.perform(get(LOGIN_ENDPOINT)
              .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return unauthorized for correct username but wrong password")
    void login_withCorrectUsernameWrongPassword_returnsUnauthorized() throws Exception {
      mockMvc.perform(get(LOGIN_ENDPOINT)
              .with(httpBasic(ADMIN_USERNAME, "wrongpassword")))
          .andExpect(status().isUnauthorized());
    }


}
