package eu.bbmri_eric.quality.agent.user;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

  private static final String LOGIN_ENDPOINT = "/api/login";
  private static final String PASSWORD_CHANGE_ENDPOINT = "/api/user/password";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";
  private static final String OTHER_USER = "user";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @WithUserDetails("admin")
  @Test
  void login_correctAuth_ok() throws Exception {
    mockMvc
        .perform(get(LOGIN_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is(ADMIN_USER)))
        .andExpect(jsonPath("$.defaultPassword", is(true)));
  }

  @WithUserDetails("admin")
  @Test
  void login_returnsDefaultPasswordFalse_whenPasswordChanged() throws Exception {
    // First change the admin password
    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    String originalPassword = adminUser.getPassword();
    adminUser.setPassword(passwordEncoder.encode("newPassword123"));
    userRepository.save(adminUser);

    mockMvc
        .perform(get(LOGIN_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is(ADMIN_USER)))
        .andExpect(jsonPath("$.defaultPassword", is(false)));

    // Reset password for other tests
    adminUser.setPassword(originalPassword);
    userRepository.save(adminUser);
  }

  @Test
  void login_returnsDefaultPasswordFalse_whenNonAdminUser() throws Exception {
    User testUser =
        userRepository.save(new User("testuser", passwordEncoder.encode("password123")));

    mockMvc
        .perform(get(LOGIN_ENDPOINT).with(httpBasic("testuser", "password123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("testuser")))
        .andExpect(jsonPath("$.defaultPassword", is(false)));

    userRepository.delete(testUser);
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

  @WithUserDetails("admin")
  @Test
  void changePassword_success_whenValidRequest() throws Exception {
    PasswordChangeRequest request =
        new PasswordChangeRequest(ADMIN_PASS, "newPass123_", "newPass123_");

    mockMvc
        .perform(
            put(PASSWORD_CHANGE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success", is(true)))
        .andExpect(jsonPath("$.message", is("Password changed successfully")));

    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    adminUser.setPassword(passwordEncoder.encode(ADMIN_PASS));
    userRepository.save(adminUser);
  }

  @WithUserDetails("admin")
  @Test
  void changePassword_failure_whenCurrentPasswordIncorrect() throws Exception {
    PasswordChangeRequest request =
        new PasswordChangeRequest("wrongCurrentPass", "newPass123_", "newPass123_");

    mockMvc
        .perform(
            put(PASSWORD_CHANGE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.message", is("Current password is incorrect")));
  }

  @WithUserDetails("admin")
  @Test
  void changePassword_failure_whenPasswordsDoNotMatch() throws Exception {
    PasswordChangeRequest request =
        new PasswordChangeRequest(ADMIN_PASS, "newPass123_", "differentPass");

    mockMvc
        .perform(
            put(PASSWORD_CHANGE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.message", is("New password and confirmation do not match")));
  }

  @WithUserDetails("admin")
  @Test
  void changePassword_failure_whenPasswordValidationFails() throws Exception {
    PasswordChangeRequest request = new PasswordChangeRequest(ADMIN_PASS, "pass", "pass");

    mockMvc
        .perform(
            put(PASSWORD_CHANGE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(
            jsonPath(
                "$.message",
                is(
                    "Password must be at least 8 characters long and contain only letters, digits or special characters")));
  }

  @Test
  void changePassword_unauthorized_whenNoAuthentication() throws Exception {
    PasswordChangeRequest request =
        new PasswordChangeRequest("currentPass", "newPass123_", "newPass123_");

    mockMvc
        .perform(
            put(PASSWORD_CHANGE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void changePassword_unauthorized_whenWrongCredentials() throws Exception {
    PasswordChangeRequest request =
        new PasswordChangeRequest("currentPass", "newPass123_", "newPass123_");

    mockMvc
        .perform(
            put(PASSWORD_CHANGE_ENDPOINT)
                .with(httpBasic("wronguser", "wrongpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
