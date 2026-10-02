package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.user.LoginAttemptService;
import eu.bbmri_eric.quality.agent.user.dto.LoginRequest;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that logging in and out produces audit log entries: successful logins and logouts via
 * {@code @Audited} on {@code AuthenticationContextService}, failed logins via {@code
 * LoginFailureAuditListener}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginAuditIntegrationTest {

  private static final String AUTH_LOGIN_ENDPOINT = "/api/auth/login";
  private static final String AUTH_LOGOUT_ENDPOINT = "/api/auth/logout";
  private static final String USER_MODULE = "user";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";
  private static final String TEST_IP = "127.0.0.1";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private LoginAttemptService loginAttemptService;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    loginAttemptService.recordSuccess(TEST_IP);
  }

  @Test
  void login_correctCredentials_recordsLoginSuccessAuditEntry() throws Exception {
    Long adminId = login().get("user").get("userId").asLong();

    List<AuditLogEntry> entries = auditLogRepository.findAll();
    assertThat(entries)
        .anySatisfy(
            entry -> {
              assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
              assertThat(entry.getActor()).isEqualTo(ADMIN_USER);
              assertThat(entry.getActorId()).isEqualTo(adminId);
              assertThat(entry.getModule()).isEqualTo(USER_MODULE);
              assertThat(entry.getEntityId()).isEqualTo(adminId);
              assertThat(entry.getDetails()).isEqualTo("Logged in");
            });
    assertThat(entries)
        .filteredOn(entry -> entry.getAction() == AuditAction.LOGIN_SUCCESS)
        .hasSize(1);
  }

  @Test
  void login_wrongPassword_recordsLoginFailureAuditEntry() throws Exception {
    LoginRequest loginRequest = new LoginRequest(ADMIN_USER, "wrongpassword");

    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());

    List<AuditLogEntry> entries = auditLogRepository.findAll();
    assertThat(entries)
        .anySatisfy(
            entry -> {
              assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_FAILURE);
              assertThat(entry.getActor()).isEqualTo(ADMIN_USER);
              assertThat(entry.getModule()).isEqualTo(USER_MODULE);
              assertThat(entry.getDetails()).isEqualTo("Bad credentials");
            });
  }

  @Test
  void logout_withValidToken_recordsLogoutAuditEntry() throws Exception {
    JsonNode loginResponse = login();
    Long adminId = loginResponse.get("user").get("userId").asLong();

    mockMvc
        .perform(
            post(AUTH_LOGOUT_ENDPOINT)
                .header("Authorization", "Bearer " + loginResponse.get("token").asText()))
        .andExpect(status().isNoContent());

    List<AuditLogEntry> entries = auditLogRepository.findAll();
    assertThat(entries)
        .anySatisfy(
            entry -> {
              assertThat(entry.getAction()).isEqualTo(AuditAction.LOGOUT);
              assertThat(entry.getActor()).isEqualTo(ADMIN_USER);
              assertThat(entry.getActorId()).isEqualTo(adminId);
              assertThat(entry.getModule()).isEqualTo(USER_MODULE);
              assertThat(entry.getEntityId()).isEqualTo(adminId);
              assertThat(entry.getDetails()).isEqualTo("Logged out");
            });
  }

  private JsonNode login() throws Exception {
    String response =
        mockMvc
            .perform(
                post(AUTH_LOGIN_ENDPOINT)
                    .with(
                        req -> {
                          req.setRemoteAddr(TEST_IP);
                          return req;
                        })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(new LoginRequest(ADMIN_USER, ADMIN_PASS))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response);
  }
}
