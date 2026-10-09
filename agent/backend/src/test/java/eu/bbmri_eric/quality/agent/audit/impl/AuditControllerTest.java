package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin")
class AuditControllerTest {

  private static final String AUDIT_LOGS_ENDPOINT = "/api/audit-logs";
  private static final String EXPORT_ENDPOINT = "/api/audit-logs/export";
  private static final String ACTIONS_ENDPOINT = "/api/audit-logs/actions";

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

  @Test
  void findAll_filteredByDateRange_returnsOnlyEntriesWithinRange() throws Exception {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Before range")
            .timestamp(LocalDateTime.of(2026, 9, 1, 23, 59, 59))
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("Within range")
            .timestamp(LocalDateTime.of(2026, 9, 15, 10, 30, 0))
            .build());
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", null)
            .details("After range")
            .timestamp(LocalDateTime.of(2026, 10, 1, 0, 0, 0))
            .build());

    mockMvc
        .perform(
            get(AUDIT_LOGS_ENDPOINT)
                .param("dateFrom", "2026-09-02T00:00:00")
                .param("dateTo", "2026-09-30T23:59:59"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.audit-logs.length()").value(1))
        .andExpect(jsonPath("$._embedded.audit-logs[0].details").value("Within range"));
  }

  @Test
  @WithAnonymousUser
  void exportCsv_withoutAuthentication_returnsUnauthorized() throws Exception {
    mockMvc.perform(get(EXPORT_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  void exportCsv_returnsCsvAttachmentWithHeaderAndRows() throws Exception {
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGIN_SUCCESS)
            .actor("admin", 1L)
            .module("user")
            .entityId(1L)
            .details("Logged in")
            .timestamp(LocalDateTime.of(2026, 9, 15, 10, 30, 0))
            .build());

    String csv =
        performExport(get(EXPORT_ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        "attachment; filename=\"audit-log-" + LocalDate.now() + ".csv\""))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(csv.lines().toList())
        .hasSize(2)
        .last()
        .asString()
        .endsWith(",2026-09-15T10:30:00,admin,1,LOGIN_SUCCESS,user,1,Logged in");
  }

  @Test
  void exportCsv_appliesFiltersButNotPagination() throws Exception {
    for (int i = 0; i < 3; i++) {
      auditRecorder.record(
          AuditRecord.of(AuditAction.LOGIN_SUCCESS)
              .actor("admin", null)
              .details("Login " + i)
              .build());
    }
    auditRecorder.record(
        AuditRecord.of(AuditAction.LOGOUT).actor("admin", null).details("Logged out").build());

    String csv =
        performExport(
                get(EXPORT_ENDPOINT)
                    .param("action", "LOGIN_SUCCESS")
                    .param("page", "0")
                    .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(csv.lines().skip(1).toList())
        .hasSize(3)
        .allSatisfy(line -> assertThat(line).contains(",LOGIN_SUCCESS,"));
  }

  @Test
  @WithAnonymousUser
  void findActions_withoutAuthentication_returnsUnauthorized() throws Exception {
    mockMvc.perform(get(ACTIONS_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  void findActions_returnsAllAuditActionsInDeclarationOrder() throws Exception {
    mockMvc
        .perform(get(ACTIONS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(AuditAction.values().length))
        .andExpect(jsonPath("$[0]").value(AuditAction.values()[0].name()))
        .andExpect(jsonPath("$[*]").value(Matchers.hasItem("OTHER")));
  }

  private ResultActions performExport(RequestBuilder request) throws Exception {
    MvcResult result = mockMvc.perform(request).andExpect(request().asyncStarted()).andReturn();
    return mockMvc.perform(asyncDispatch(result));
  }
}
