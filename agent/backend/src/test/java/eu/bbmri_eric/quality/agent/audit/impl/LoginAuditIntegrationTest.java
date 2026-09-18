package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.user.LoginAttemptService;
import eu.bbmri_eric.quality.agent.user.dto.LoginRequest;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that logging in produces audit log entries via Spring Security's built-in authentication
 * auditing (see {@code SecurityConfig#authenticationEventPublisher} and {@code
 * AuditConfig#authenticationAuditListener}), rather than through code in {@code AuthController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginAuditIntegrationTest {

  private static final String AUTH_LOGIN_ENDPOINT = "/api/auth/login";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";
  private static final String TEST_IP = "127.0.0.1";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private LoginAttemptService loginAttemptService;

  @AfterEach
  void tearDown() {
    loginAttemptService.recordSuccess(TEST_IP);
  }

  @Test
  void login_correctCredentials_recordsLoginSuccessAuditEntry() throws Exception {
    LoginRequest loginRequest = new LoginRequest(ADMIN_USER, ADMIN_PASS);

    mockMvc
        .perform(
            post(AUTH_LOGIN_ENDPOINT)
                .with(
                    req -> {
                      req.setRemoteAddr(TEST_IP);
                      return req;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk());

    List<AuditLogEntry> entries = auditLogRepository.findAll();
    assertThat(entries)
        .anySatisfy(
            entry -> {
              assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
              assertThat(entry.getActor()).isEqualTo(ADMIN_USER);
            });
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
            });
  }
}
