package eu.bbmri_eric.quality.server.user;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.common.auth.LoginRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserControllerTest {

  private static final String AUTH_LOGIN_ENDPOINT = "/api/auth/login";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @AfterEach
  void tearDown() {
    User adminUser =
        userRepository.findByUsername(ADMIN_USER).orElseThrow(EntityNotFoundException::new);
    adminUser.setPassword(passwordEncoder.encode(ADMIN_PASS));
    userRepository.save(adminUser);
  }

  @Test
  void login_correctCredentials_returnsTokenAndUserInfo() throws Exception {
    LoginRequest loginRequest = new LoginRequest(ADMIN_USER, ADMIN_PASS);
    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.user.username", is(ADMIN_USER)));
  }

  @Test
  void login_invalidCredentials_returnsUnauthorized() throws Exception {
    LoginRequest loginRequest = new LoginRequest(ADMIN_USER, "wrongpassword");

    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_invalidUsername_returnsUnauthorized() throws Exception {
    LoginRequest loginRequest = new LoginRequest("nonexistent", ADMIN_PASS);

    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_emptyCredentials_returnsBadRequest() throws Exception {
    LoginRequest loginRequest = new LoginRequest("", "");

    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_nullCredentials_returnsBadRequest() throws Exception {
    mockMvc
        .perform(post(AUTH_LOGIN_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_invalidContentType_returnsUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT).contentType(MediaType.TEXT_PLAIN).content("invalid content"))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void login_malformedJson_returnsBadRequest() throws Exception {
    String malformedJson = "{invalid json";
    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changePassword_withValidToken_returnsOk() throws Exception {
    String token = authenticateAndGetToken();

    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    Long adminUserId = adminUser.getId();

    String newPassword = "newPass123!";
    PasswordChangeRequest request =
        new PasswordChangeRequest("adminpass", newPassword, newPassword);

    mockMvc
        .perform(
            put("/api/users/" + adminUserId + "/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
    LoginRequest loginRequest = new LoginRequest(ADMIN_USER, newPassword);
    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.user.username", is(ADMIN_USER)));
  }

  @Test
  void changePassword_withInvalidToken_returnsUnauthorized() throws Exception {
    PasswordChangeRequest request =
        new PasswordChangeRequest("current", "newPass123!", "newPass123!");

    mockMvc
        .perform(
            put("/api/users/1/password")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void changePassword_withoutToken_returnsUnauthorized() throws Exception {
    PasswordChangeRequest request =
        new PasswordChangeRequest("current", "newPass123!", "newPass123!");

    mockMvc
        .perform(
            put("/api/users/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void changePassword_withValidToken_invalidPasswordFormat_returnsBadRequest() throws Exception {
    String token = authenticateAndGetToken();

    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    Long adminUserId = adminUser.getId();

    PasswordChangeRequest request = new PasswordChangeRequest("adminpass", "short", "short");

    mockMvc
        .perform(
            put("/api/users/" + adminUserId + "/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changePassword_tryingToChangeOtherUserPassword_returnsForbidden() throws Exception {
    String token = authenticateAndGetToken();
    long otherUserId = -1L;

    PasswordChangeRequest request =
        new PasswordChangeRequest("adminpass", "newPass123!", "newPass123!");

    mockMvc
        .perform(
            put("/api/users/" + otherUserId + "/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void tokenGeneration_validLoginReturnsJwtToken() throws Exception {
    String token = authenticateAndGetToken();

    // Verify the token is not null and has the expected JWT structure
    assertNotNull(token, "Token should not be null");
    assertEquals(3, token.split("\\.").length, "JWT token should have 3 parts separated by dots");
    assertTrue(
        token.startsWith("eyJ"), "JWT token should start with 'eyJ' (Base64 encoded header)");
  }

  @Test
  void tokenAuthentication_validTokenAllowsApiAccess() throws Exception {
    String token = authenticateAndGetToken();

    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    Long adminUserId = adminUser.getId();

    // Use the token to access a protected endpoint
    mockMvc
        .perform(
            put("/api/users/" + adminUserId + "/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new PasswordChangeRequest("adminpass", "newPass123!", "newPass123!"))))
        .andExpect(status().isOk());
  }

  @Test
  void tokenAuthentication_expiredOrInvalidTokenDeniesAccess() throws Exception {
    // Test with completely invalid token
    mockMvc
        .perform(
            put("/api/users/1/password")
                .header("Authorization", "Bearer totally-invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new PasswordChangeRequest("current", "newPass123!", "newPass123!"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getAllUsers_withValidToken_returnsUsersList() throws Exception {
    String token = authenticateAndGetToken();

    mockMvc
        .perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").exists())
        .andExpect(jsonPath("$._embedded.userDTOList").isArray());
  }

  @Test
  void getAllUsers_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void getAllUsers_withInvalidToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/users").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getUserById_withValidTokenAndExistingUser_returnsUser() throws Exception {
    String token = authenticateAndGetToken();
    User adminUser = userRepository.findByUsername(ADMIN_USER).orElseThrow();
    Long adminUserId = adminUser.getId();

    mockMvc
        .perform(get("/api/v1/users/" + adminUserId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is(ADMIN_USER)))
        .andExpect(jsonPath("$.id").value(adminUserId));
  }

  @Test
  void getUserById_withValidTokenAndNonExistingUser_returnsNotFound() throws Exception {
    String token = authenticateAndGetToken();
    long nonExistingUserId = 999999L;

    mockMvc
        .perform(
            get("/api/v1/users/" + nonExistingUserId).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void getUserById_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/users/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void getUserById_withInvalidToken_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/1").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  /** Helper method to authenticate and extract JWT token from response */
  private String authenticateAndGetToken() throws Exception {
    LoginRequest loginRequest = new LoginRequest(ADMIN_USER, ADMIN_PASS);

    MvcResult result =
        mockMvc
            .perform(
                post(AUTH_LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

    String responseContent = result.getResponse().getContentAsString();
    var response = objectMapper.readTree(responseContent);
    return response.get("token").asText();
  }
}
