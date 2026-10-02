package eu.bbmri_eric.quality.agent.audit.impl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin")
class AuditControllerTest {

  private static final String AUDIT_LOGS_ENDPOINT = "/api/audit-logs";

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditRecorderImpl auditRecorder;
  @Autowired private AuditLogRepository auditLogRepository;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @Test
  @WithAnonymousUser
  void findAll_withoutAuthentication_returnsUnauthorized() throws Exception {
    mockMvc.perform(get(AUDIT_LOGS_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  void findAll_noEntries_returnsEmptyPage() throws Exception {
    mockMvc
        .perform(get(AUDIT_LOGS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$.page.totalElements").value(0));
  }

  @Test
  void findAll_withEntries_returnsEmbeddedList() throws Exception {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Logged in")
            .build());

    mockMvc
        .perform(get(AUDIT_LOGS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.audit-logs").isArray())
        .andExpect(jsonPath("$._embedded.audit-logs.length()").value(1))
        .andExpect(jsonPath("$._embedded.audit-logs[0].action").value("LOGIN_SUCCESS"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void findAll_filteredByAction_returnsOnlyMatchingEntries() throws Exception {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Logged in")
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGOUT).actor("admin", null).details("Logged out").build());

    mockMvc
        .perform(get(AUDIT_LOGS_ENDPOINT).param("action", "LOGOUT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.audit-logs.length()").value(1))
        .andExpect(jsonPath("$._embedded.audit-logs[0].action").value("LOGOUT"));
  }

  @Test
  void findAll_filteredByActor_returnsOnlyMatchingEntries() throws Exception {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Logged in")
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_FAILURE)
            .actor("intruder", null)
            .details("Bad password")
            .build());

    mockMvc
        .perform(get(AUDIT_LOGS_ENDPOINT).param("actor", "intruder"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.audit-logs.length()").value(1))
        .andExpect(jsonPath("$._embedded.audit-logs[0].actor").value("intruder"));
  }

  @Test
  void findAll_withSearch_matchesDetailsCaseInsensitively() throws Exception {
    auditRecorder.record(
        AuditRecord.of(AuditAction.SETTINGS_UPDATED)
            .actor("admin", null)
            .details("Changed FHIR URL")
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGOUT).actor("admin", null).details("Logged out").build());

    mockMvc
        .perform(get(AUDIT_LOGS_ENDPOINT).param("search", "fhir"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.audit-logs.length()").value(1))
        .andExpect(jsonPath("$._embedded.audit-logs[0].details").value("Changed FHIR URL"));
  }

  @Test
  void findAll_withPagination_returnsPagedResults() throws Exception {
    for (int i = 0; i < 3; i++) {
      auditRecorder.record(
          AuditRecord.of(AuditAction.LOGIN_SUCCESS)
              .actor("admin", null)
              .details("Login " + i)
              .build());
    }

    mockMvc
        .perform(get(AUDIT_LOGS_ENDPOINT).param("page", "0").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.audit-logs.length()").value(2))
        .andExpect(jsonPath("$.page.totalElements").value(3))
        .andExpect(jsonPath("$.page.totalPages").value(2))
        .andExpect(jsonPath("$._links.next").exists());
  }
}
