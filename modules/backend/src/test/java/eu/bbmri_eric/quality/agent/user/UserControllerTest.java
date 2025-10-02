package eu.bbmri_eric.quality.agent.user;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserDetailService userDetailService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @AfterEach
  void tearDown() {
    User adminUser = userRepository.findByUsername(ADMIN_USER).orElse(null);
    if (adminUser != null) {
      adminUser.setPassword(passwordEncoder.encode(ADMIN_PASS));
      userRepository.save(adminUser);
    }
  }

  @WithUserDetails("admin")
  @Test
  void login_correctAuth_ok() throws Exception {
    mockMvc
        .perform(get(LOGIN_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is(ADMIN_USER)))
        .andExpect(jsonPath("$.defaultPassword", is(true)));
  }

  @Test
  void login_returnsUnauthorized_whenNotAuthenticated() throws Exception {
    mockMvc.perform(get(LOGIN_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @WithUserDetails("admin")
  @Test
  void changePassword_returnsCorrectHttpStatus_whenValidRequest() throws Exception {
    Long adminUserId = userDetailService.getUserId(ADMIN_USER);
    PasswordChangeRequest request =
        new PasswordChangeRequest("adminpass", "newPass123!", "newPass123!");

    mockMvc
        .perform(
            put("/api/users/" + adminUserId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @WithUserDetails("admin")
  @Test
  void changePassword_returnsBadRequest_whenInvalidPasswordFormat() throws Exception {
    Long adminUserId = userDetailService.getUserId(ADMIN_USER);
    PasswordChangeRequest request = new PasswordChangeRequest("adminpass", "short", "short");

    mockMvc
        .perform(
            put("/api/users/" + adminUserId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @WithUserDetails("admin")
  @Test
  void changePassword_returnsForbidden_whenTryingToChangeOtherUserPassword() throws Exception {
    Long otherUserId = -1L;
    PasswordChangeRequest request =
        new PasswordChangeRequest("adminpass", "newPass123!", "newPass123!");

    mockMvc
        .perform(
            put("/api/users/" + otherUserId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void changePassword_returnsUnauthorized_whenNotAuthenticated() throws Exception {
    PasswordChangeRequest request =
        new PasswordChangeRequest("current", "newPass123!", "newPass123!");

    mockMvc
        .perform(
            put("/api/users/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @WithUserDetails("admin")
  @Test
  void changePassword_rejectsInvalidContentType() throws Exception {
    Long adminUserId = userDetailService.getUserId(ADMIN_USER);

    mockMvc
        .perform(
            put("/api/users/" + adminUserId + "/password")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content"))
        .andExpect(status().isUnsupportedMediaType());
  }
}
