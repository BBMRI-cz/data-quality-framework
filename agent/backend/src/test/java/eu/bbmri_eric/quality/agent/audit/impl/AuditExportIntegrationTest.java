package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.user.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Exercises the CSV export against a real servlet container. The export is streamed, which
 * completes in an async dispatch that MockMvc does not reproduce faithfully: there the security
 * context survives the dispatch, whereas a real container re-runs the security filter chain without
 * the bearer token having been re-authenticated.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditExportIntegrationTest {

  private static final String EXPORT_ENDPOINT = "/api/audit-logs/export";

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private AuditRecorderImpl auditRecorder;
  @Autowired private AuditLogRepository auditLogRepository;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @Test
  void exportCsv_withBearerToken_streamsCsv() {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGOUT).actor("admin", null).details("Logged out").build());
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authenticateAndGetToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            EXPORT_ENDPOINT, HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains(",LOGOUT,,,Logged out");
  }

  @Test
  void exportCsv_withoutToken_returnsUnauthorized() {
    ResponseEntity<String> response = restTemplate.getForEntity(EXPORT_ENDPOINT, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private String authenticateAndGetToken() {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/api/auth/login", new LoginRequest("admin", "adminpass"), JsonNode.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody().get("token").asText();
  }
}
