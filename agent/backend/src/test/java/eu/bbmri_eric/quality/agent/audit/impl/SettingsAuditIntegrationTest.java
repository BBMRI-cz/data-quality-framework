package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.settings.DatabaseType;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
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

/** Verifies that updating the settings produces an audit log entry via {@code @Audited}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SettingsAuditIntegrationTest {

  private static final String AUTH_LOGIN_ENDPOINT = "/api/auth/login";
  private static final String SETTINGS_ENDPOINT = "/api/settings";
  private static final String SETTINGS_MODULE = "settings";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";
  private static final String TEST_IP = "127.0.0.1";
  private static final String SYSTEM_ACTOR = "SYSTEM";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private LoginAttemptService loginAttemptService;
  @Autowired private SettingsService settingsService;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    loginAttemptService.recordSuccess(TEST_IP);
  }

  @Test
  void updateSettings_authenticated_recordsSettingsUpdatedAuditEntry() throws Exception {
    JsonNode loginResponse = login();
    Long adminId = loginResponse.get("user").get("userId").asLong();

    mockMvc
        .perform(
            put(SETTINGS_ENDPOINT)
                .header("Authorization", "Bearer " + loginResponse.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSettingsDTO())))
        .andExpect(status().isOk());

    List<AuditLogEntry> entries =
        auditLogRepository.findAll().stream()
            .filter(entry -> entry.getAction() == AuditAction.SETTINGS_UPDATED)
            .toList();
    assertThat(entries)
        .singleElement()
        .satisfies(
            entry -> {
              assertThat(entry.getActor()).isEqualTo(ADMIN_USER);
              assertThat(entry.getActorId()).isEqualTo(adminId);
              assertThat(entry.getModule()).isEqualTo(SETTINGS_MODULE);
              assertThat(entry.getEntityId()).isNull();
              assertThat(entry.getDetails()).isEqualTo("Settings updated");
            });
  }

  @Test
  void updateSettings_invalidSettings_doesNotRecordAuditEntry() throws Exception {
    JsonNode loginResponse = login();
    SettingsDTO dto = validSettingsDTO();
    dto.setEpsilon(2.0);

    mockMvc
        .perform(
            put(SETTINGS_ENDPOINT)
                .header("Authorization", "Bearer " + loginResponse.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    assertThat(auditLogRepository.findAll())
        .noneMatch(entry -> entry.getAction() == AuditAction.SETTINGS_UPDATED);
  }

  @Test
  void updateSettings_withoutAuthenticatedUser_recordsSystemActor() {
    settingsService.updateSettings(validSettingsDTO());

    assertThat(auditLogRepository.findAll())
        .filteredOn(entry -> entry.getAction() == AuditAction.SETTINGS_UPDATED)
        .singleElement()
        .satisfies(
            entry -> {
              assertThat(entry.getActor()).isEqualTo(SYSTEM_ACTOR);
              assertThat(entry.getActorId()).isNull();
              assertThat(entry.getModule()).isEqualTo(SETTINGS_MODULE);
            });
  }

  private SettingsDTO validSettingsDTO() {
    return SettingsDTO.builder()
        .fhirUrl("http://localhost:8080/fhir")
        .fhirUsername("testuser")
        .fhirPassword("dGVzdHBhc3M=")
        .epsilon(0.5)
        .delta(1.0E-8)
        .minThreshold(20)
        .noiseMechanism(NoiseMechanism.GAUSSIAN)
        .databaseType(DatabaseType.FHIR)
        .build();
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
