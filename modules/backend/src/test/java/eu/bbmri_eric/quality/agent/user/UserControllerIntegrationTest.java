package eu.bbmri_eric.quality.agent.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestUserSeedConfig.class)
@Transactional
class UserControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void changePassword_ReturnsSuccessWhenValidRequest() throws Exception {
    Map<String, String> request =
        Map.of(
            "currentPassword", "pass",
            "newPassword", "newPassword123",
            "confirmPassword", "newPassword123");

    mockMvc
        .perform(
            post("/api/user/change-password")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Password changed successfully"));
  }

  @Test
  void changePassword_ReturnsBadRequestWhenValidationFails() throws Exception {
    Map<String, String> request =
        Map.of(
            "currentPassword", "pass",
            "newPassword", "short",
            "confirmPassword", "short");

    mockMvc
        .perform(
            post("/api/user/change-password")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("New password must be at least 8 characters long"));
  }

  @Test
  void changePassword_ReturnsBadRequestWhenCurrentPasswordIncorrect() throws Exception {
    Map<String, String> request =
        Map.of(
            "currentPassword", "wrongPassword",
            "newPassword", "newPassword123",
            "confirmPassword", "newPassword123");

    mockMvc
        .perform(
            post("/api/user/change-password")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("Current password is incorrect"));
  }

  @Test
  void changePassword_ReturnsBadRequestWhenPasswordsDoNotMatch() throws Exception {
    Map<String, String> request =
        Map.of(
            "currentPassword", "pass",
            "newPassword", "newPassword123",
            "confirmPassword", "differentPassword");

    mockMvc
        .perform(
            post("/api/user/change-password")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("New password and confirmation do not match"));
  }

  @Test
  void changePassword_HandlesEmptyRequestBody() throws Exception {
    Map<String, String> request = Map.of();

    mockMvc
        .perform(
            post("/api/user/change-password")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("Current password is required"));
  }

  @Test
  void changePassword_RequiresAuthentication() throws Exception {
    Map<String, String> request =
        Map.of(
            "currentPassword", "pass",
            "newPassword", "newPassword123",
            "confirmPassword", "newPassword123");

    mockMvc
        .perform(
            post("/api/user/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
