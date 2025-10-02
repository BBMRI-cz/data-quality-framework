package eu.bbmri_eric.quality.server.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for JWT authentication using MockMvc with @SpringBootTest. Tests the
 * /api/auth/login endpoint for JWT token generation.
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
    String loginJson =
        """
        {
          "username": "%s",
          "password": "%s"
        }
        """
            .formatted(ADMIN_USERNAME.toUpperCase(), ADMIN_PASSWORD);

    mockMvc
        .perform(post(LOGIN_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(loginJson))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return JWT token for successful login with correct credentials")
  void login_withCorrectCredentials_returnsOk() throws Exception {
    String loginJson =
        """
        {
          "username": "%s",
          "password": "%s"
        }
        """
            .formatted(ADMIN_USERNAME, ADMIN_PASSWORD);

    mockMvc
        .perform(post(LOGIN_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(loginJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.user.username").value(ADMIN_USERNAME));
  }

  @Test
  @DisplayName("Should return unauthorized for correct username but wrong password")
  void login_withCorrectUsernameWrongPassword_returnsUnauthorized() throws Exception {
    String loginJson =
        """
        {
          "username": "%s",
          "password": "%s"
        }
        """
            .formatted(ADMIN_USERNAME, "wrongpassword");

    mockMvc
        .perform(post(LOGIN_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(loginJson))
        .andExpect(status().isUnauthorized());
  }
}
