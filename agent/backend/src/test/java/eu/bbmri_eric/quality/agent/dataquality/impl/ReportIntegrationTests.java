package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin")
public class ReportIntegrationTests {
  public static final String API_REPORTS = "/api/reports";
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ReportRepository reportRepository;

  @Test
  void testCreateReportWithStatusGeneratingSetsGeneratedAt() throws Exception {
    Report report = new Report();
    String json = objectMapper.writeValueAsString(report);
    String location =
        mockMvc
            .perform(post(API_REPORTS).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse()
            .getHeader("Location");
    assertNotNull(location);
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(
            () ->
                mockMvc
                    .perform(get(location).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("GENERATED"))
                    .andExpect(jsonPath("$.generatedAt").exists()));
  }

  @Test
  void testGetReportsWithPagination() throws Exception {

    // Create 15 reports to ensure pagination
    for (int i = 0; i < 15; i++) {
      reportRepository.save(new Report());
    }

    mockMvc
        .perform(get(API_REPORTS).param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.reports").isArray())
        .andExpect(jsonPath("$._embedded.reports.length()").value(10))
        .andExpect(jsonPath("$.page.size").value(10))
        .andExpect(jsonPath("$.page.totalElements").isNotEmpty())
        .andExpect(jsonPath("$.page.totalPages").isNotEmpty())
        .andExpect(jsonPath("$._links.next").exists());

    mockMvc
        .perform(get(API_REPORTS).param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.reports").isArray())
        .andExpect(
            jsonPath("$._embedded.reports.length()")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
  }

  @Test
  void testGetReportsReturnsEmbeddedList() throws Exception {
    mockMvc
        .perform(get(API_REPORTS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.reports").exists());
  }
}
